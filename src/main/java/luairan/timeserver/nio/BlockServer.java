package luairan.timeserver.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * nio �������
 * 
 * @author airan.lar
 *
 */
public class BlockServer {
	ServerSocketChannel serverSocketChannel;
	ExecutorService executorService;

	public BlockServer() throws IOException {
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors());
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().setReuseAddress(true);
		serverSocketChannel.socket().bind(new InetSocketAddress(9000));
		System.out.println("Server open");
	}

	public void service() throws IOException {
		while (true) {
			SocketChannel socketChannel = serverSocketChannel.accept();
			executorService.execute(new Handler(socketChannel));
		}
	}

	public static void main(String[] args) throws IOException {
		new BlockServer().service();
	}

}

class Handler implements Runnable {
	private SocketChannel socketChannels;

	Handler(SocketChannel socketChannel) {
		this.socketChannels = socketChannel;
	}

	@Override
	public void run() {
		handle(socketChannels);
	}

	private void handle(SocketChannel socketChannel) {
		try {
			Socket socket = socketChannel.socket();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			String msg = null;
			while ((msg = br.readLine()) != null) {
				System.out.println(msg);
				pw.println(msg);
				pw.flush();
				if (msg.equals("quit")) {
					break;
				}
			}
		} catch (Exception e) {

		} finally {
			try {
				if (socketChannel != null)
					socketChannel.close();
			} catch (Exception e2) {

			}
		}

	}
}