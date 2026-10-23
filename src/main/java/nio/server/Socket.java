package nio.server;

import io.support.AsyncEchoHandler;

public class Socket {
    public static void main(String[] args) throws Exception {
        AsyncEchoHandler.serve(args.length == 0 ? 9999 : Integer.parseInt(args[0]), false);
    }
}
