package luairan.timeserver.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
/**
 * �򵥵Ļ���java BIO �׽��� cilent
 * @author airan.lar
 *
 */
public class Client {
	public static void main(String[] args) throws UnknownHostException,
			IOException {
		@SuppressWarnings("resource")
		Socket socket = new Socket("localhost", 9000);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		BufferedWriter write = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("input you want to send to server");
		System.out.println("type 'quit' to exit");
		while (true) {
			String input = scanner.nextLine();
			write.write(input + "\n");
			write.flush();
			String out = reader.readLine();
			if (out.equals("ok") && input.equals("quit"))
				break;
			System.out.println("���������ص���Ϣ" + out);
		}
		reader.close();
		write.close();
	}
}
