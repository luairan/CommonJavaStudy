package nioSample;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

final class Handler implements Runnable {
    final SocketChannel socket;
    final SelectionKey sk;
    final ByteBuffer input = ByteBuffer.allocate(8192);
    static final int READING = 0, SENDING = 1;
    int state = READING;
    Handler(Selector selector, SocketChannel socket) throws IOException {
        this.socket = socket;
        socket.configureBlocking(false);
        sk = socket.register(selector,SelectionKey.OP_READ,this);
    }
    public void run() {
        try {
            if (state == READING) {
                int count = socket.read(input);
                if (count < 0) { close(); return; }
                if (count == 0) return;
                input.flip(); state = SENDING;
                sk.interestOps(SelectionKey.OP_WRITE);
            } else {
                socket.write(input);
                if (!input.hasRemaining()) {
                    input.clear(); state = READING;
                    sk.interestOps(SelectionKey.OP_READ);
                }
            }
        } catch (IOException e) { close(); }
    }
    private void close() {
        sk.cancel();
        try { socket.close(); } catch (IOException ignored) { }
    }
}
