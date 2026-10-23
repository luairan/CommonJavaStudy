package nioSample.nio.asyn.multithread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Worker implements Runnable {
    private final BlockingQueue<EventData> queue = new ArrayBlockingQueue<>(1024);
    public void processData(EventData event) {
        if (!queue.offer(event)) throw new IllegalStateException("Worker queue is full");
    }
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                EventData event = queue.take();
                System.out.println("Client event is:" + event.getMessage());
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
