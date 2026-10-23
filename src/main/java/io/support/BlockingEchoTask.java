package io.support;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BlockingEchoTask implements Runnable {
    private final Socket socket;
    public BlockingEchoTask(Socket socket) { this.socket = socket; }
    public void run() {
        try (Socket client = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                boolean quit = line.equals("quit");
                out.write(quit ? "ok\n" : "server: " + client.getRemoteSocketAddress() + "\t input \t :" + line + "\n");
                out.flush();
                if (quit) return;
            }
        } catch (IOException e) { System.err.println("Client disconnected: " + e.getMessage()); }
    }
}
