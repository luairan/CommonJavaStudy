import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"unchecked", "rawtypes"})
public class RegressionTest {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        sorting();
        queues();
        semaphoreRollback();
        http();
        tls(args[0]);
        nio();
        System.out.println("PASS: sorting, prime, queue isolation/cancellation, semaphore rollback, HTTP/TLS, NIO");
    }

    static void sorting() throws Exception {
        Random random = new Random(1);
        for (int round = 0; round < 100; round++) {
            int[] values = new int[round * 13];
            for (int i = 0; i < values.length; i++) values[i] = random.nextInt(101) - 50;
            int[] expected = values.clone(), merged = values.clone();
            Arrays.sort(expected);
            new sort.QuickSort().qSort(values, 0, values.length - 1);
            new sort.MergeSort().mergeSort(merged, 0, merged.length - 1);
            check(Arrays.equals(values, expected), "quick sort");
            check(Arrays.equals(merged, expected), "merge sort");
        }
        for (int mode = 0; mode < 3; mode++) {
            int[] values = new int[500000];
            for (int i = 0; i < values.length; i++) values[i] = mode == 0 ? i : mode == 1 ? -i : 7;
            new sort.QuickSort().qSort(values, 0, values.length - 1);
            for (int i = 1; i < values.length; i++) check(values[i-1] <= values[i], "large sort");
        }
        thread.Interrupt prime = new thread.Interrupt();
        Method method = thread.Interrupt.class.getDeclaredMethod("isPrime", long.class);
        method.setAccessible(true);
        for (long n : new long[]{-1, 0, 1, 4, 9, 49}) check(!(Boolean) method.invoke(prime,n), "not prime " + n);
        for (long n : new long[]{2, 3, 5, 97}) check((Boolean) method.invoke(prime,n), "prime " + n);
    }

    interface Interruptible { void run() throws Exception; }
    static Thread blocked(Interruptible operation, AtomicReference<Throwable> failure) {
        Thread worker = new Thread(() -> {
            try { operation.run(); failure.set(new AssertionError("operation did not block")); }
            catch (InterruptedException expected) { }
            catch (Throwable t) { failure.set(t); }
        });
        worker.setDaemon(true);
        worker.start();
        return worker;
    }
    static void awaitBlocked(Thread worker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (worker.getState() != Thread.State.WAITING && System.nanoTime() < deadline) Thread.sleep(1);
        check(worker.getState() == Thread.State.WAITING, "worker failed to block");
    }
    static void cancel(Thread worker, AtomicReference<Throwable> failure) throws Exception {
        worker.interrupt(); worker.join(3000);
        check(!worker.isAlive(), "worker ignored interruption");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    static void queues() throws Exception {
        for (int version = 1; version <= 2; version++) {
            Class<?> type = Class.forName("thread.producercostomer.ProducerCustomer" + version);
            Class<?> taskType = Class.forName(type.getName() + "$Task");
            Object first = type.getConstructor().newInstance(), second = type.getConstructor().newInstance();
            Object task = taskType.getConstructor(String.class).newInstance("first");
            Method put = type.getMethod("put", taskType), take = type.getMethod("take");
            put.invoke(first, task);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread worker = blocked(() -> invoke(take, second), failure);
            awaitBlocked(worker); cancel(worker, failure);
            check(take.invoke(first) == task, "queue instances interfere");
            for (int i = 0; i < 10; i++) put.invoke(first, task);
            worker = blocked(() -> invoke(put, first, task), failure);
            awaitBlocked(worker); cancel(worker, failure);
            for (int i = 0; i < 10; i++) check(take.invoke(first) == task, "queue contents");
        }
    }
    static Object invoke(Method method, Object target, Object... args) throws Exception {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) { throw (Exception) e.getCause(); }
    }

    static void semaphoreRollback() throws Exception {
        thread.producercostomer.ProducerCustomer4 queue = new thread.producercostomer.ProducerCustomer4();
        Class<?> type = queue.getClass();
        Field field = type.getDeclaredField("mutex"); field.setAccessible(true);
        Semaphore mutex = (Semaphore) field.get(queue);
        for (boolean producer : new boolean[]{true, false}) {
            if (!producer) queue.put("saved");
            mutex.acquire();
            try {
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread worker = blocked(() -> { if (producer) queue.put("x"); else queue.take(); }, failure);
                awaitBlocked(worker); cancel(worker, failure);
            } finally { mutex.release(); }
            if (!producer) check(queue.take().equals("saved"), "interrupted consumer lost item");
            Field slots = type.getDeclaredField("notfull"); slots.setAccessible(true);
            check(((Semaphore) slots.get(queue)).availablePermits() == 10, "slot permit leaked");
            check(mutex.availablePermits() == 1, "mutex permit leaked");
        }
    }

    static void http() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String text = exchange.getRequestURI().toString() + "|" + new String(body, StandardCharsets.UTF_8);
            byte[] reply = text.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(reply); }
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/?a=1#fragment";
            for (String pkg : new String[]{"util", "luairan.http.client"}) {
                Class<?> paramsType = Class.forName(pkg + ".HttpsParam");
                Object params = paramsType.getConstructor().newInstance();
                check(params.toString().isEmpty(), "empty parameters");
                paramsType.getMethod("putParam",String.class,String.class).invoke(params,"name","中文 &");
                Class<?> utils = Class.forName(pkg + ".HttpsUtils");
                Class<? extends Enum> methodType = (Class<? extends Enum>) Class.forName(pkg + ".HttpsUtils$Method");
                Method request = utils.getMethod("doUrl",String.class,paramsType,methodType);
                String get = new String((byte[]) request.invoke(null,url,params,Enum.valueOf(methodType,"GET")),StandardCharsets.UTF_8);
                check(get.equals("/?a=1&name=%E4%B8%AD%E6%96%87+%26|"), "GET query " + get);
                String post = new String((byte[]) request.invoke(null,url,params,Enum.valueOf(methodType,"POST")),StandardCharsets.UTF_8);
                check(post.equals("/?a=1|name=%E4%B8%AD%E6%96%87+%26"), "POST body " + post);
                request.invoke(null,url,null,Enum.valueOf(methodType,"GET"));
            }
        } finally { server.stop(0); }
    }

    static void tls(String keystore) throws Exception {
        java.security.KeyStore keys = java.security.KeyStore.getInstance("PKCS12");
        char[] password = "changeit".toCharArray();
        try (InputStream in = new FileInputStream(keystore)) { keys.load(in, password); }
        javax.net.ssl.KeyManagerFactory km = javax.net.ssl.KeyManagerFactory.getInstance(
                javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        km.init(keys,password);
        javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
        context.init(km.getKeyManagers(),null,null);
        com.sun.net.httpserver.HttpsServer server = com.sun.net.httpserver.HttpsServer.create(
                new InetSocketAddress("localhost",0),0);
        server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(context));
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200,1);
            try (OutputStream out = exchange.getResponseBody()) { out.write('x'); }
            exchange.close();
        });
        server.start();
        javax.net.ssl.SSLSocketFactory original = javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory();
        try {
            assertTlsRejected(server.getAddress().getPort()); // Self-signed certificate is untrusted.
            java.security.KeyStore trust = java.security.KeyStore.getInstance("PKCS12");
            trust.load(null,null);
            trust.setCertificateEntry("test",keys.getCertificate("test"));
            javax.net.ssl.TrustManagerFactory tm = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tm.init(trust);
            javax.net.ssl.SSLContext trusted = javax.net.ssl.SSLContext.getInstance("TLS");
            trusted.init(null,tm.getTrustManagers(),null);
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(trusted.getSocketFactory());
            assertTlsRejected(server.getAddress().getPort()); // Trusted certificate has the wrong hostname.
        } finally {
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(original);
            server.stop(0);
        }
    }

    static void assertTlsRejected(int port) throws Exception {
        for (String pkg : new String[]{"util", "luairan.http.client"}) {
            Class<?> utils = Class.forName(pkg + ".HttpsUtils");
            Class<?> params = Class.forName(pkg + ".HttpsParam");
            Class<? extends Enum> method = (Class<? extends Enum>) Class.forName(pkg + ".HttpsUtils$Method");
            try {
                utils.getMethod("doUrl",String.class,params,method).invoke(null,
                        "https://localhost:" + port + "/",null,Enum.valueOf(method,"GET"));
                throw new AssertionError("invalid TLS identity accepted");
            } catch (InvocationTargetException e) {
                check(e.getCause() instanceof javax.net.ssl.SSLHandshakeException,
                        "unexpected TLS failure: " + e.getCause());
            }
        }
    }

    static void nio() throws Exception {
        try (java.nio.channels.Selector selector = java.nio.channels.Selector.open();
             java.nio.channels.ServerSocketChannel listener = java.nio.channels.ServerSocketChannel.open()) {
            listener.bind(new InetSocketAddress("localhost",0)); listener.configureBlocking(false);
            listener.register(selector,java.nio.channels.SelectionKey.OP_ACCEPT);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread reactor = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        selector.select();
                        Iterator<java.nio.channels.SelectionKey> keys = selector.selectedKeys().iterator();
                        while (keys.hasNext()) {
                            java.nio.channels.SelectionKey key = keys.next(); keys.remove();
                            if (!key.isValid()) continue;
                            if (key.isAcceptable()) luairan.textserver.nio.Server.accept(key,selector);
                            else {
                                if (key.isReadable()) luairan.textserver.nio.Server.read(key,selector);
                                if (key.isValid() && key.isWritable()) luairan.textserver.nio.Server.write(key,selector);
                            }
                        }
                    }
                } catch (Throwable t) { failure.set(t); }
            });
            reactor.setDaemon(true); reactor.start();
            try (Socket client = new Socket()) {
                client.setReceiveBufferSize(1024);
                client.connect(listener.getLocalAddress()); client.setSoTimeout(10000);
                OutputStream out = client.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
                byte[] prefix = "  中".getBytes(StandardCharsets.UTF_8);
                out.write(prefix, 0, 3); out.flush();
                Thread.sleep(30);
                out.write(prefix, 3, prefix.length - 3);
                out.write("文  \r\nfirst\nsecond\n".getBytes(StandardCharsets.UTF_8)); out.flush();
                check("  中文  ".equals(in.readLine()), "fragment/whitespace");
                check("first".equals(in.readLine()) && "second".equals(in.readLine()), "coalesced lines");
                String large = String.join("", Collections.nCopies(60000,"x"));
                out.write((large+"\n").getBytes(StandardCharsets.UTF_8)); out.flush();
                Thread.sleep(50);
                check(large.equals(in.readLine()), "large slow-reader response");
                out.write("qu".getBytes(StandardCharsets.UTF_8)); out.flush(); Thread.sleep(20);
                out.write("it\nignored\n".getBytes(StandardCharsets.UTF_8)); out.flush();
                check("ok".equals(in.readLine()), "split quit");
                check(in.readLine() == null, "quit must close");
            } finally {
                reactor.interrupt(); selector.wakeup(); reactor.join(3000);
                for (java.nio.channels.SelectionKey key : selector.keys()) key.channel().close();
            }
            if (failure.get() != null) throw new AssertionError(failure.get());
        }
    }
}
