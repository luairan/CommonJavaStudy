package io.aio;

import io.support.AsyncEchoHandler;

public class Socket {
    public static void main(String[] args) throws Exception {
        AsyncEchoHandler.serve(args.length == 0 ? 9000 : Integer.parseInt(args[0]), true);
    }
}
