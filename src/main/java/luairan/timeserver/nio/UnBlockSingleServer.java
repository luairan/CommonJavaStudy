package luairan.timeserver.nio;

import java.io.IOException;

/** Historical duplicate entry point, sharing the tested NIO line implementation. */
public class UnBlockSingleServer extends io.nio.UnBlockSingleServer {
    public UnBlockSingleServer() throws IOException { super(); }
    public UnBlockSingleServer(int port) throws IOException { super(port); }
    public static void main(String[] args) throws IOException {
        new UnBlockSingleServer(args.length == 0 ? 9000 : Integer.parseInt(args[0])).service();
    }
}
