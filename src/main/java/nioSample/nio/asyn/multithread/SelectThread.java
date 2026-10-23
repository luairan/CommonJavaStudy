package nioSample.nio.asyn.multithread;

import io.support.LineProcessorServer;
import java.io.*;
import java.util.Collections;

public class SelectThread implements Runnable {
    private final LineProcessorServer server;
    public SelectThread(Worker worker) { this(worker,8090); }
    public SelectThread(Worker worker, int port) {
        try {
            server = new LineProcessorServer(new int[]{port},
                    Collections.singletonList(line -> worker.processData(new EventData(line))));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void run() { server.run(); }
    public static void main(String[] args) {
        Worker worker = new Worker();
        SelectThread selector = new SelectThread(worker,args.length == 0 ? 8090 : Integer.parseInt(args[0]));
        Thread thread = new Thread(worker); thread.start();
        try { selector.run(); }
        finally { thread.interrupt(); }
    }
}
