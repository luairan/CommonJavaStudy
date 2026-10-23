package nioSample.nio.asyn;

import io.support.LineProcessorServer;
import java.io.*;
import java.util.Arrays;

public class OperationServer implements Runnable {
    private final LineProcessorServer server;
    public OperationServer() { this(8090,9090); }
    public OperationServer(int addPort, int multiplyPort) {
        try {
            server = new LineProcessorServer(new int[]{addPort,multiplyPort},
                    Arrays.asList(new AddProcessor()::process,new MultiProcessor()::process));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void run() { server.run(); }
    public static void main(String[] args) {
        new OperationServer(args.length > 0 ? Integer.parseInt(args[0]) : 8090,
                args.length > 1 ? Integer.parseInt(args[1]) : 9090).run();
    }
}
