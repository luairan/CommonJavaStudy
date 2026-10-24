package luairan.timeserver.nio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;

public class BlockServer implements AutoCloseable {
    private final ServerSocketChannel listener;
    private final ExecutorService executor;
    private volatile boolean closed;
    private final Set<SocketChannel> clients = ConcurrentHashMap.newKeySet();
    public BlockServer() throws IOException { this(9000); }
    public BlockServer(int port) throws IOException {
        int size = Math.max(2,Runtime.getRuntime().availableProcessors());
        executor = new ThreadPoolExecutor(size,size,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));
        listener = ServerSocketChannel.open();
        try {
            listener.setOption(StandardSocketOptions.SO_REUSEADDR,true);
            listener.bind(new InetSocketAddress("localhost",port));
        }
        catch (IOException | RuntimeException e) { close(); throw e; }
    }
    public void service() throws IOException {
        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                SocketChannel client = listener.accept();
                synchronized (this) {
                if (closed) { client.close(); break; }
                clients.add(client);
                try {
                    executor.execute(() -> {
                        try { new Handler(client).run(); }
                        finally { clients.remove(client); }
                    });
                } catch (RejectedExecutionException e) { clients.remove(client); client.close(); }
                }
            }
        } catch (AsynchronousCloseException e) {
            if (!closed && !Thread.currentThread().isInterrupted()) throw e;
        } finally { close(); }
    }
    public int getPort() throws IOException {
        return ((InetSocketAddress) listener.getLocalAddress()).getPort();
    }
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout,unit);
    }
    public synchronized void close() throws IOException {
        closed = true;
        try { listener.close(); }
        finally {
            try { for (SocketChannel client : clients) client.close(); }
            finally { executor.shutdownNow(); }
        }
    }
    public static void main(String[] args) throws IOException {
        new BlockServer(args.length == 0 ? 9000 : Integer.parseInt(args[0])).service();
    }
}
class Handler implements Runnable {
    private final SocketChannel channel;
    Handler(SocketChannel channel) { this.channel = channel; }
    public void run() {
        try (SocketChannel client = channel;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.socket().getInputStream(),StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.socket().getOutputStream(),StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.write(line + "\n"); out.flush();
                if (line.equals("quit")) break;
            }
        } catch (IOException e) { System.err.println("Client disconnected: " + e.getMessage()); }
    }
}
