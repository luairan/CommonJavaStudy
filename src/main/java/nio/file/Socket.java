package nio.file;

public class Socket {
    public Socket(int port) {
        try { io.support.AsyncEchoHandler.serve(port, false); }
        catch (Exception e) { throw new IllegalStateException("Cannot run echo server", e); }
    }
    public static void main(String[] args) {
        new Socket(args.length == 0 ? 8000 : Integer.parseInt(args[0]));
    }
}
