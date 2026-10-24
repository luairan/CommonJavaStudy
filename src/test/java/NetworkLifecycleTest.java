import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import nioSample.nio.sample.echo.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 15, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class NetworkLifecycleTest {
    enum PeerMode { SILENT_ECHO, DRIP_ECHO, HTTP_HEADERS, HTTP_BODY }
    enum ServiceKind { NIO_LINE, LEGACY_NIO_LINE, PROCESSOR, BLOCKING_LINE, NIO_RAW }
    enum Shutdown { CLOSE, INTERRUPT, CONCURRENT_CLOSE }

    /** A local peer that accepts real TCP connections but deliberately withholds completion. */
    static final class StallingPeer implements AutoCloseable {
        final ServerSocket listener = new ServerSocket(0,50,InetAddress.getLoopbackAddress());
        final AtomicReference<Socket> accepted = new AtomicReference<>();
        final CountDownLatch received = new CountDownLatch(1), release = new CountDownLatch(1);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<?> task;
        final PeerMode mode;

        StallingPeer(PeerMode mode) throws IOException {
            this.mode = mode;
            task = executor.submit(() -> {
                try (Socket socket = listener.accept()) {
                    accepted.set(socket);
                    if (release.getCount() == 0) return;
                    socket.setSoTimeout(1000);
                    if (mode == PeerMode.HTTP_HEADERS || mode == PeerMode.HTTP_BODY) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                        if (mode == PeerMode.HTTP_BODY) {
                            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n"
                                    + "Connection: close\r\n\r\nx").getBytes(StandardCharsets.US_ASCII));
                            socket.getOutputStream().flush();
                        }
                    } else socket.getInputStream().readNBytes(4);
                    received.countDown();
                    if (mode == PeerMode.DRIP_ECHO) {
                        while (!release.await(40,TimeUnit.MILLISECONDS)) {
                            socket.getOutputStream().write('x'); socket.getOutputStream().flush();
                        }
                    } else release.await();
                } catch (IOException e) {
                    // Deadline expiry or fixture shutdown closes the peer's socket.
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        int port() { return listener.getLocalPort(); }
        String url() { return "http://localhost:" + port() + "/stall"; }
        void awaitRequest() throws InterruptedException {
            assertTrue(received.await(3,TimeUnit.SECONDS),"request never reached the peer");
        }
        public void close() throws Exception {
            release.countDown(); listener.close();
            Socket socket = accepted.get(); if (socket != null) socket.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS),"peer worker leaked");
            task.get(1,TimeUnit.SECONDS);
        }
    }

    static final class ObservedResponse extends RspHandler {
        final CompletableFuture<Object> outcome = new CompletableFuture<>();
        public synchronized boolean handleResponse(byte[] bytes) {
            super.handleResponse(bytes); outcome.complete(bytes); return true;
        }
        public synchronized void handleFailure(Throwable error) {
            super.handleFailure(error); outcome.complete(error);
        }
        Throwable awaitFailure() throws Exception {
            Object value = outcome.get(3,TimeUnit.SECONDS);
            assertInstanceOf(Throwable.class,value,"request unexpectedly succeeded");
            assertThrows(IllegalStateException.class,this::waitForResponse);
            return (Throwable) value;
        }
    }

    interface ThrowingTask { void run() throws Exception; }
    static final class RunningTask implements AutoCloseable {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch stopped = new CountDownLatch(1);
        final Thread thread;
        boolean started;
        RunningTask(ThrowingTask action) {
            thread = new Thread(() -> {
                try { action.run(); } catch (Throwable e) { failure.set(e); }
                finally { stopped.countDown(); }
            },"network-lifecycle-service");
            thread.setDaemon(true);
        }
        void start() { started = true; thread.start(); }
        void awaitStopped() throws Exception {
            assertTrue(stopped.await(3,TimeUnit.SECONDS),"service did not stop");
            thread.join(1000);
            assertFalse(thread.isAlive(),"service thread leaked");
            assertNull(failure.get(),() -> "service shutdown threw: " + failure.get());
        }
        public void close() throws Exception {
            if (started) { thread.interrupt(); awaitStopped(); }
        }
    }

    @ParameterizedTest
    @EnumSource(value = PeerMode.class,names = {"SILENT_ECHO","DRIP_ECHO"})
    void echoDeadlineDoesNotWaitForeverOrResetOnPartialData(PeerMode mode) throws Exception {
        try (StallingPeer peer = new StallingPeer(mode);
             NioClient client = new NioClient(InetAddress.getLoopbackAddress(),peer.port(),500);
             RunningTask runner = new RunningTask(client::run)) {
            runner.start();
            ObservedResponse response = new ObservedResponse();
            client.send(new byte[1024],response);
            peer.awaitRequest();
            assertInstanceOf(SocketTimeoutException.class,response.awaitFailure());
            assertTrue(runner.thread.isAlive(),"one timeout must not stop the reactor");
            peer.release.countDown(); peer.task.get(3,TimeUnit.SECONDS);
            Future<?> healthyPeer = peer.executor.submit(() -> {
                try (Socket socket = peer.listener.accept()) {
                    peer.accepted.set(socket); socket.setSoTimeout(2000);
                    socket.getOutputStream().write(socket.getInputStream().readNBytes(4));
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            ObservedResponse healthy = new ObservedResponse(); byte[] data = {1,2,3,4};
            client.send(data,healthy);
            assertArrayEquals(data,(byte[]) healthy.outcome.get(3,TimeUnit.SECONDS));
            healthyPeer.get(3,TimeUnit.SECONDS);
            client.close(); runner.awaitStopped();
        }
    }

    @Test
    void closingClientWakesPendingRequestAndReleasesItsThread() throws Exception {
        try (StallingPeer peer = new StallingPeer(PeerMode.SILENT_ECHO);
             NioClient client = new NioClient(InetAddress.getLoopbackAddress(),peer.port());
             RunningTask runner = new RunningTask(client::run)) {
            runner.start(); ObservedResponse response = new ObservedResponse();
            client.send(new byte[4],response); peer.awaitRequest();
            client.close(); client.close();
            assertInstanceOf(IOException.class,response.awaitFailure());
            runner.awaitStopped();
            assertThrows(IOException.class,() -> client.send(new byte[4],new RspHandler()));
        }
    }

    @Test
    void closingClientBeforeStartingItsReactorCancelsQueuedRequest() throws Exception {
        try (StallingPeer peer = new StallingPeer(PeerMode.SILENT_ECHO);
             NioClient client = new NioClient(InetAddress.getLoopbackAddress(),peer.port());
             RunningTask runner = new RunningTask(client::run)) {
            ObservedResponse response = new ObservedResponse(); client.send(new byte[4],response);
            client.close(); assertInstanceOf(IOException.class,response.awaitFailure());
            runner.start(); runner.awaitStopped();
        }
    }

    @Test
    void queuedRequestsExpireWithoutWaitingForNewSocketEvents() throws Exception {
        try (StallingPeer peer = new StallingPeer(PeerMode.SILENT_ECHO);
             NioClient client = new NioClient(InetAddress.getLoopbackAddress(),peer.port(),100);
             RunningTask runner = new RunningTask(client::run)) {
            ObservedResponse response = new ObservedResponse(); client.send(new byte[4],response);
            Thread.sleep(200); // Deliberately keep the reactor stopped beyond the request deadline.
            runner.start();
            assertInstanceOf(SocketTimeoutException.class,response.awaitFailure());
            client.close(); runner.awaitStopped();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"util","luairan.http.client"})
    void httpTimesOutBeforeResponseHeaders(String packageName) throws Exception {
        try (StallingPeer peer = new StallingPeer(PeerMode.HTTP_HEADERS)) {
            assertThrows(SocketTimeoutException.class,() -> httpGet(packageName,peer.url(),250));
            peer.awaitRequest();
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"util","luairan.http.client"})
    void httpTimesOutDuringIncompleteResponseBody(String packageName) throws Exception {
        try (StallingPeer peer = new StallingPeer(PeerMode.HTTP_BODY)) {
            assertThrows(SocketTimeoutException.class,() -> httpGet(packageName,peer.url(),250));
            peer.awaitRequest();
        }
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    static byte[] httpGet(String packageName, String url, int readTimeout) throws Exception {
        Class<?> type = Class.forName(packageName + ".HttpsUtils");
        Class<? extends Enum> method = (Class<? extends Enum>) Class.forName(packageName + ".HttpsUtils$Method");
        try {
            return (byte[]) type.getMethod("doUrl",String.class,ByteArrayOutputStream.class,String.class,
                    method,int.class,int.class).invoke(null,url,null,"UTF-8",Enum.valueOf(method,"GET"),1000,readTimeout);
        } catch (InvocationTargetException e) { throw (Exception) e.getCause(); }
    }

    @Test
    void nonPositiveTimeoutsAreRejectedBeforeOpeningConnections() throws Exception {
        assertThrows(IllegalArgumentException.class,() -> new NioClient(InetAddress.getLoopbackAddress(),1,0));
        assertThrows(IllegalArgumentException.class,() -> new NioClient(InetAddress.getLoopbackAddress(),1,-1));
        for (String pkg : new String[]{"util","luairan.http.client"}) {
            assertThrows(IllegalArgumentException.class,() -> httpGet(pkg,"http://localhost:1/",0));
        }
    }

    static final class Service implements AutoCloseable {
        final AutoCloseable endpoint;
        final int port;
        final RunningTask runner;
        final RunningTask worker;
        final BlockingQueue<String> messages;
        Service(ServiceKind kind, int port) throws Exception {
            RunningTask workerTask = null; BlockingQueue<String> queue = null;
            switch (kind) {
                case NIO_LINE, LEGACY_NIO_LINE -> {
                    io.nio.UnBlockSingleServer server = kind == ServiceKind.NIO_LINE
                            ? new io.nio.UnBlockSingleServer(port)
                            : new luairan.timeserver.nio.UnBlockSingleServer(port);
                    endpoint = server; this.port = server.getPort(); runner = new RunningTask(server::service);
                }
                case PROCESSOR -> {
                    queue = new LinkedBlockingQueue<>(); BlockingQueue<String> destination = queue;
                    io.support.LineProcessorServer server = new io.support.LineProcessorServer(new int[]{port},
                            Collections.singletonList(destination::add));
                    endpoint = server; this.port = server.getPort(0); runner = new RunningTask(server::run);
                }
                case BLOCKING_LINE -> {
                    luairan.timeserver.nio.BlockServer server = new luairan.timeserver.nio.BlockServer(port);
                    endpoint = server; this.port = server.getPort(); runner = new RunningTask(server::service);
                }
                case NIO_RAW -> {
                    EchoWorker echo = new EchoWorker(); workerTask = new RunningTask(echo::run);
                    NioServer server = new NioServer(InetAddress.getLoopbackAddress(),port,echo);
                    endpoint = server; this.port = server.getPort(); runner = new RunningTask(server::run);
                }
                default -> throw new AssertionError(kind);
            }
            worker = workerTask; messages = queue;
        }
        void start() { if (worker != null) worker.start(); runner.start(); }
        Socket connect() throws IOException {
            Socket socket = new Socket();
            try { socket.connect(new InetSocketAddress("localhost",port),1000); socket.setSoTimeout(2000); return socket; }
            catch (IOException e) { socket.close(); throw e; }
        }
        void confirmAccepted(Socket socket) throws Exception {
            socket.getOutputStream().write("hello\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            if (messages != null) assertEquals("hello",messages.poll(3,TimeUnit.SECONDS));
            else assertEquals("hello",new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)).readLine());
        }
        public void close() throws Exception {
            try { endpoint.close(); }
            finally {
                try { runner.close(); }
                finally {
                    if (worker != null) worker.close();
                    if (endpoint instanceof luairan.timeserver.nio.BlockServer server)
                        assertTrue(server.awaitTermination(3,TimeUnit.SECONDS),"blocking workers leaked");
                }
            }
        }
    }

    static void assertDisconnected(Socket socket) throws IOException {
        try { assertEquals(-1,socket.getInputStream().read(),"connection remained open after shutdown"); }
        catch (SocketTimeoutException e) { fail("shutdown did not close the client connection",e); }
        catch (SocketException expected) { /* Closing a live socket may send a reset. */ }
    }

    @ParameterizedTest
    @EnumSource(ServiceKind.class)
    void closeStopsLiveServiceAndAllowsRestart(ServiceKind kind) throws Exception {
        checkShutdown(kind,Shutdown.CLOSE);
    }
    @ParameterizedTest
    @EnumSource(ServiceKind.class)
    void interruptStopsLiveServiceAndAllowsRestart(ServiceKind kind) throws Exception {
        checkShutdown(kind,Shutdown.INTERRUPT);
    }
    @ParameterizedTest
    @EnumSource(ServiceKind.class)
    void concurrentCloseIsIdempotent(ServiceKind kind) throws Exception {
        checkShutdown(kind,Shutdown.CONCURRENT_CLOSE);
    }
    static void checkShutdown(ServiceKind kind, Shutdown shutdown) throws Exception {
        try (Service service = new Service(kind,0)) {
            service.start();
            try (Socket active = service.connect(); Socket idle = service.connect()) {
                service.confirmAccepted(active);
                switch (shutdown) {
                    case CLOSE -> service.endpoint.close();
                    case INTERRUPT -> service.runner.thread.interrupt();
                    case CONCURRENT_CLOSE -> {
                        ExecutorService closers = Executors.newFixedThreadPool(3);
                        try {
                            List<Future<?>> tasks = new ArrayList<>();
                            for (int i = 0; i < 3; i++) tasks.add(closers.submit(() -> {
                                try { service.endpoint.close(); } catch (Exception e) { throw new CompletionException(e); }
                            }));
                            for (Future<?> task : tasks) task.get(3,TimeUnit.SECONDS);
                        } finally { closers.shutdownNow(); assertTrue(closers.awaitTermination(3,TimeUnit.SECONDS)); }
                    }
                }
                service.runner.awaitStopped();
                assertDisconnected(active); assertDisconnected(idle);
                service.close();
                try (Service restarted = new Service(kind,service.port)) {
                    restarted.start();
                    try (Socket healthy = restarted.connect()) { restarted.confirmAccepted(healthy); }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ServiceKind.class)
    void closingBeforeRunDoesNotLeakOrThrow(ServiceKind kind) throws Exception {
        try (Service service = new Service(kind,0)) {
            service.endpoint.close(); service.endpoint.close();
            service.start(); service.runner.awaitStopped();
        }
    }

    @ParameterizedTest
    @EnumSource(ServiceKind.class)
    void abruptlyDisconnectedClientsDoNotStopService(ServiceKind kind) throws Exception {
        try (Service service = new Service(kind,0)) {
            service.start();
            for (int i = 0; i < 8; i++) {
                try (Socket broken = service.connect()) {
                    broken.setSoLinger(true,0);
                    broken.getOutputStream().write("partial".getBytes(StandardCharsets.UTF_8));
                }
            }
            try (Socket healthy = service.connect()) { service.confirmAccepted(healthy); }
        }
    }
}
