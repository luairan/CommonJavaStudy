package luairan.textserver.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Iterator;

/** UTF-8 line protocol: LF or CRLF terminates a message; quit replies ok then closes. */
public class Server {
    private static final int MAX_LINE_BYTES = 64 * 1024;

    private static final class Connection {
        final ByteBuffer input = ByteBuffer.allocate(8192);
        final ByteArrayOutputStream line = new ByteArrayOutputStream();
        final ArrayDeque<ByteBuffer> output = new ArrayDeque<>();
        boolean closeAfterWrite;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 9000 : Integer.parseInt(args[0]);
        try (Selector selector = Selector.open();
             ServerSocketChannel server = ServerSocketChannel.open()) {
            server.socket().setReuseAddress(true);
            server.configureBlocking(false);
            server.bind(new InetSocketAddress("localhost", port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();
                        if (!key.isValid()) continue;
                        try {
                            if (key.isAcceptable()) accept(key, selector);
                            else {
                                if (key.isReadable()) read(key, selector);
                                if (key.isValid() && key.isWritable()) write(key, selector);
                            }
                        } catch (IOException e) {
                            close(key);
                        }
                    }
                }
            } finally {
                for (SelectionKey key : selector.keys()) close(key);
            }
        }
    }

    private static void close(SelectionKey key) throws IOException {
        key.attach(null);
        key.cancel();
        key.channel().close();
    }

    public static void accept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        if (channel == null) return;
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ, new Connection());
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    public static void read(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Connection state = (Connection) key.attachment();
        int count = channel.read(state.input);
        if (count == 0) return;
        if (count == -1) {
            // An unterminated final line is not a complete message.
            if (state.output.isEmpty()) close(key);
            else {
                state.closeAfterWrite = true;
                key.interestOps(SelectionKey.OP_WRITE);
            }
            return;
        }
        state.input.flip();
        while (state.input.hasRemaining()) {
            byte value = state.input.get();
            if (value == '\n') {
                byte[] bytes = state.line.toByteArray();
                state.line.reset();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') length--;
                String message = new String(bytes, 0, length, StandardCharsets.UTF_8);
                boolean quit = message.equals("quit");
                state.output.add(ByteBuffer.wrap((quit ? "ok\n" : message + "\n")
                        .getBytes(StandardCharsets.UTF_8)));
                if (quit) {
                    state.closeAfterWrite = true;
                    break;
                }
            } else {
                if (state.line.size() == MAX_LINE_BYTES) {
                    throw new IOException("Line exceeds " + MAX_LINE_BYTES + " bytes");
                }
                state.line.write(value);
            }
        }
        state.input.clear();
        // Pause reads while responses are pending, bounding queued output for slow readers.
        if (!state.output.isEmpty()) key.interestOps(SelectionKey.OP_WRITE);
    }

    public static void write(SelectionKey key, Selector selector) throws IOException {
        Connection state = (Connection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        while (!state.output.isEmpty()) {
            ByteBuffer response = state.output.peek();
            channel.write(response);
            if (response.hasRemaining()) return;
            state.output.remove();
        }
        if (state.closeAfterWrite) close(key);
        else key.interestOps(SelectionKey.OP_READ);
    }
}
