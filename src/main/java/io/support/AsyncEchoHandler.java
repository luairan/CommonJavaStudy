package io.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Callback-only AIO: never block the completion thread waiting for another operation. */
public class AsyncEchoHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
    private final boolean lines;
    public AsyncEchoHandler(boolean lines) { this.lines = lines; }

    @Override
    public void completed(AsynchronousSocketChannel client, AsynchronousServerSocketChannel server) {
        // Re-arm accept immediately so an idle client cannot block every other client.
        if (server.isOpen()) server.accept(server, this);
        new Session(client, lines).read();
    }

    @Override
    public void failed(Throwable failure, AsynchronousServerSocketChannel server) {
        if (server.isOpen()) failure.printStackTrace();
    }

    private static class Session {
        final AsynchronousSocketChannel client;
        final boolean lines;
        final ByteBuffer input = ByteBuffer.allocate(8192);
        final ByteArrayOutputStream line = new ByteArrayOutputStream();
        boolean quit;
        Session(AsynchronousSocketChannel client, boolean lines) { this.client = client; this.lines = lines; }

        void close() {
            try { client.close(); } catch (IOException ignored) { }
        }

        void read() {
            input.clear();
            client.read(input, null, new CompletionHandler<Integer, Void>() {
                public void completed(Integer count, Void unused) {
                    if (count < 0) { close(); return; }
                    if (count == 0) { read(); return; }
                    input.flip();
                    if (!lines) { write(input); return; }
                    ByteArrayOutputStream replies = new ByteArrayOutputStream();
                    try {
                        while (input.hasRemaining()) {
                            byte value = input.get();
                            if (value != '\n') {
                                if (line.size() >= 65536) throw new IOException("Line too long");
                                line.write(value);
                            } else {
                                byte[] bytes = line.toByteArray(); line.reset();
                                int length = bytes.length;
                                if (length > 0 && bytes[length - 1] == '\r') length--;
                                String text = new String(bytes, 0, length, StandardCharsets.UTF_8);
                                quit = text.equals("quit");
                                String reply = quit ? "ok\n" : "server: " + client.getLocalAddress()
                                        + "\t input \t :" + text + "\n";
                                replies.write(reply.getBytes(StandardCharsets.UTF_8));
                                if (quit) break;
                            }
                        }
                        if (replies.size() == 0) read();
                        else write(ByteBuffer.wrap(replies.toByteArray()));
                    } catch (IOException e) { close(); }
                }
                public void failed(Throwable failure, Void unused) { close(); }
            });
        }

        void write(ByteBuffer output) {
            client.write(output, output, new CompletionHandler<Integer, ByteBuffer>() {
                public void completed(Integer count, ByteBuffer buffer) {
                    if (buffer.hasRemaining()) write(buffer);
                    else if (quit) close();
                    else read();
                }
                public void failed(Throwable failure, ByteBuffer buffer) { close(); }
            });
        }
    }

    public static void serve(int port, boolean lines) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group)) {
            server.setOption(java.net.StandardSocketOptions.SO_REUSEADDR,true);
            server.bind(new InetSocketAddress("localhost", port));
            server.accept(server, new AsyncEchoHandler(lines));
            System.out.println("Listening on " + server.getLocalAddress());
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } finally {
            group.shutdownNow();
            executor.shutdownNow();
        }
    }
}
