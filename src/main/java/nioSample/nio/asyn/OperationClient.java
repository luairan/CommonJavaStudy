package nioSample.nio.asyn;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class OperationClient {
    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8090 : Integer.parseInt(args[0]);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        try (Socket socket = new Socket("localhost",port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) { out.write(line + "\n"); out.flush(); }
        }
    }
}
