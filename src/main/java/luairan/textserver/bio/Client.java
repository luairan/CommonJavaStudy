package luairan.textserver.bio;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", args.length == 0 ? 9000 : Integer.parseInt(args[0]));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8))) {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
            String line;
            while ((line = console.readLine()) != null) {
                out.write(line + "\n"); out.flush();
                String response = in.readLine();
                if (response == null) break;
                System.out.println(response);
                if (line.equals("quit")) break;
            }
        }
    }
}
