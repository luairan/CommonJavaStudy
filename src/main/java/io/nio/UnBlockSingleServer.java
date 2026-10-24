package io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/** Uses the shared line-protocol handlers rather than permanently registering OP_WRITE. */
public class UnBlockSingleServer implements AutoCloseable {
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private volatile boolean closed;
    public UnBlockSingleServer() throws IOException { this(9000); }
    public UnBlockSingleServer(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        try {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.setOption(java.net.StandardSocketOptions.SO_REUSEADDR,true);
            serverSocketChannel.bind(new InetSocketAddress("localhost",port));
            serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT);
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }
    public void service() throws IOException {
        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next(); keys.remove();
                    if (!key.isValid()) continue;
                    try {
                        if (key.isAcceptable()) accept(key,selector);
                        else {
                            if (key.isReadable()) read(key,selector);
                            if (key.isValid() && key.isWritable()) write(key,selector);
                        }
                    } catch (IOException | CancelledKeyException e) { key.cancel(); key.channel().close(); }
                }
            }
        } catch (ClosedSelectorException e) {
            if (!closed) throw e;
        } catch (IOException e) {
            if (!closed) throw e;
        } finally { close(); }
    }
    public int getPort() throws IOException {
        return ((InetSocketAddress) serverSocketChannel.getLocalAddress()).getPort();
    }
    public void read(SelectionKey key, Selector selector) throws IOException {
        luairan.textserver.nio.Server.read(key,selector);
    }
    public void write(SelectionKey key, Selector selector) throws IOException {
        luairan.textserver.nio.Server.write(key,selector);
    }
    public synchronized void accept(SelectionKey key, Selector selector) throws IOException {
        if (closed) return;
        luairan.textserver.nio.Server.accept(key,selector);
    }
    public String decode(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.asReadOnlyBuffer()).toString();
    }
    public synchronized void close() throws IOException {
        closed = true;
        selector.wakeup();
        if (selector.isOpen()) {
            for (SelectionKey key : selector.keys()) key.channel().close();
            selector.close();
        }
        serverSocketChannel.close();
    }
    public static void main(String[] args) throws IOException {
        new UnBlockSingleServer(args.length == 0 ? 9000 : Integer.parseInt(args[0])).service();
    }
}
