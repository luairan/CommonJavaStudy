package luairan.textserver.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(args.length == 0 ? 9000 : Integer.parseInt(args[0]))) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = server.accept();
                new Thread(new Task(client)).start();
            }
        }
    }
}
class Task extends io.support.BlockingEchoTask {
    Task(Socket socket) { super(socket); }
}
