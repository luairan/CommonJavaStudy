package nioSample.nio.sample.echo;

import java.nio.charset.StandardCharsets;

public class RspHandler {
    private byte[] rsp;
    private Throwable failure;
    public synchronized boolean handleResponse(byte[] response) {
        rsp = response.clone(); notifyAll(); return true;
    }
    public synchronized void handleFailure(Throwable cause) { failure = cause; notifyAll(); }
    public synchronized void waitForResponse() {
        while (rsp == null && failure == null) {
            try { wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        if (failure != null) throw new IllegalStateException("Echo request failed",failure);
        System.out.println("response:" + new String(rsp,StandardCharsets.UTF_8));
    }
}
