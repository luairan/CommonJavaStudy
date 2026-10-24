import io.support.AsyncEchoHandler;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 15, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class AsyncShutdownTest {
    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void interruptClosesAioClientsListenerAndCompletionThreads(boolean lines) throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0,50,InetAddress.getLoopbackAddress())) {
            port = reservation.getLocalPort();
        }
        AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        ThreadGroup group = new ThreadGroup("aio-lifecycle-fixture") {
            public void uncaughtException(Thread thread, Throwable error) { callbackFailure.compareAndSet(null,error); }
        };
        FutureTask<Void> action = new FutureTask<>(() -> { AsyncEchoHandler.serve(port,lines); return null; });
        Thread service = new Thread(group,action,"aio-lifecycle-service");
        service.setDaemon(true); service.start();
        try {
            try (Socket active = awaitConnect(port,action); Socket idle = connect(port)) {
                byte[] request = "hello\n".getBytes(StandardCharsets.UTF_8);
                active.getOutputStream().write(request);
                if (lines) {
                    String response = new BufferedReader(new InputStreamReader(active.getInputStream(),StandardCharsets.UTF_8)).readLine();
                    assertNotNull(response); assertTrue(response.endsWith("hello"));
                } else assertArrayEquals(request,active.getInputStream().readNBytes(request.length));
                service.interrupt(); service.join(3000);
                assertFalse(service.isAlive(),"AIO service did not stop on interrupt");
                action.get(1,TimeUnit.SECONDS);
                NetworkLifecycleTest.assertDisconnected(active);
                NetworkLifecycleTest.assertDisconnected(idle);
            }
        } finally {
            service.interrupt(); service.join(3000);
        }
        assertNull(callbackFailure.get(),() -> "AIO callback failed during shutdown: " + callbackFailure.get());
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (hasLiveThreads(group) && System.nanoTime() < deadline) Thread.sleep(10);
        assertFalse(hasLiveThreads(group),"AIO completion threads survived shutdown");
        try (ServerSocket restarted = new ServerSocket()) {
            restarted.setReuseAddress(true);
            restarted.bind(new InetSocketAddress("localhost",port));
        }
    }

    private static boolean hasLiveThreads(ThreadGroup group) {
        Thread[] threads = new Thread[Math.max(32,group.activeCount()*2+1)];
        int count = group.enumerate(threads,true);
        return Arrays.stream(threads,0,count).anyMatch(Thread::isAlive);
    }
    private static Socket connect(int port) throws IOException {
        Socket socket = new Socket();
        try { socket.connect(new InetSocketAddress("localhost",port),500); socket.setSoTimeout(2000); return socket; }
        catch (IOException e) { socket.close(); throw e; }
    }
    private static Socket awaitConnect(int port, FutureTask<Void> action) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            if (action.isDone()) { action.get(); fail("AIO service stopped before accepting clients"); }
            try { return connect(port); } catch (ConnectException e) { Thread.sleep(10); }
        }
        throw new AssertionError("AIO service did not bind its listener");
    }
}
