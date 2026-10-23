package io.support;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Each accepted connection retains its own processor and unfinished UTF-8 line. */
public class LineProcessorServer implements Runnable, AutoCloseable {
    private final Selector selector;
    private final List<ServerSocketChannel> listeners = new ArrayList<>();
    private volatile boolean closed;
    private static class Input {
        final Consumer<String> processor;
        final ByteArrayOutputStream line = new ByteArrayOutputStream();
        Input(Consumer<String> processor) { this.processor = processor; }
    }
    public LineProcessorServer(int[] ports, List<Consumer<String>> processors) throws IOException {
        if (ports.length != processors.size()) throw new IllegalArgumentException("One processor per port required");
        selector = Selector.open();
        try {
            for (int i = 0; i < ports.length; i++) {
                ServerSocketChannel listener = ServerSocketChannel.open(); listeners.add(listener);
                listener.configureBlocking(false);
                listener.bind(new InetSocketAddress("localhost", ports[i]));
                listener.register(selector, SelectionKey.OP_ACCEPT, processors.get(i));
            }
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }
    public int getPort(int index) throws IOException {
        return ((InetSocketAddress) listeners.get(index).getLocalAddress()).getPort();
    }
    @SuppressWarnings("unchecked")
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next(); keys.remove();
                    if (!key.isValid()) continue;
                    try {
                        if (key.isAcceptable()) {
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            if (client != null) {
                                client.configureBlocking(false);
                                client.register(selector,SelectionKey.OP_READ,
                                        new Input((Consumer<String>) key.attachment()));
                            }
                        } else if (key.isReadable()) {
                            buffer.clear();
                            int count = ((SocketChannel) key.channel()).read(buffer);
                            if (count < 0) { key.cancel(); key.channel().close(); continue; }
                            buffer.flip();
                            Input input = (Input) key.attachment();
                            while (buffer.hasRemaining()) {
                                byte value = buffer.get();
                                if (value == '\n') {
                                    String line = new String(input.line.toByteArray(),StandardCharsets.UTF_8);
                                    input.line.reset();
                                    if (line.endsWith("\r")) line = line.substring(0,line.length()-1);
                                    input.processor.accept(line);
                                } else {
                                    if (input.line.size() >= 65536) throw new IOException("Line too long");
                                    input.line.write(value);
                                }
                            }
                        }
                    } catch (IOException | RuntimeException e) {
                        key.cancel(); key.channel().close();
                    }
                }
            }
        } catch (ClosedSelectorException e) {
            if (!closed) throw e;
        } catch (IOException e) {
            if (!closed) throw new UncheckedIOException(e);
        } finally {
            try { close(); } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }
    public synchronized void close() throws IOException {
        closed = true;
        if (selector.isOpen()) {
            selector.wakeup();
            for (SelectionKey key : selector.keys()) key.channel().close();
            selector.close();
        }
        for (ServerSocketChannel listener : listeners) listener.close();
    }
}
