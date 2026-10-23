package nioSample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Reactor implements Runnable {
	final Selector selector;
	final ServerSocketChannel serverSocket;

	public Reactor(int port) throws IOException {
		selector = Selector.open();
		serverSocket = ServerSocketChannel.open();
        try {
            serverSocket.socket().bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            SelectionKey sk = serverSocket.register(selector,SelectionKey.OP_ACCEPT);
            sk.attach(new Acceptor());
        } catch (IOException | RuntimeException e) {
            serverSocket.close(); selector.close(); throw e;
        }
	}

	public void run() { // normally in a new
		try {
			while (!Thread.interrupted()) {
				selector.select();
				Set selected = selector.selectedKeys();
				Iterator it = selected.iterator();
				while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    if (key.isValid()) dispatch(key);
                }
				selected.clear();
			}
		} catch (IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        } finally {
            try {
                for (SelectionKey key : selector.keys()) key.channel().close();
                selector.close();
            } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }
	}

	void dispatch(SelectionKey k) {
		Runnable r = (Runnable) (k.attachment());
		if (r != null)
			r.run();
	}

    class Acceptor implements Runnable {
        public void run() {
            SocketChannel client = null;
            try {
                client = serverSocket.accept();
                if (client != null) new Handler(selector,client);
            } catch (IOException e) {
                if (client != null) {
                    try { client.close(); } catch (IOException closeFailure) { e.addSuppressed(closeFailure); }
                }
                System.err.println("Cannot accept client: " + e.getMessage());
            }
        }
    }
    public static void main(String[] args) throws IOException {
        new Reactor(args.length == 0 ? 9000 : Integer.parseInt(args[0])).run();
    }
}
