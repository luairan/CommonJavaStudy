package luairan.textserver.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * �򵥵���bio��java �׽��ֱ�� ������
 * @author airan.lar
 *
 */
public class Server {
	public static void main(String[] args) throws IOException {
		@SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(9000);
		while (true) {
			Socket socket = serverSocket.accept();
			new Thread(new Task(socket)).start();
		}
	}
}

class Task implements Runnable {
	private Socket socket;

	public Task(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		BufferedReader reader = null;
		BufferedWriter write = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			write = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));
			while (true) {
				String input = reader.readLine();
				if (input.equals("quit")) {
					write.write("ok" + "\n");
					write.flush();
					break;
				}
				write.write("server: "
						+ socket.getInetAddress().getHostAddress() + ":"
						+ socket.getPort() + "\t input \t :" + input + "\n");
				write.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				write.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}