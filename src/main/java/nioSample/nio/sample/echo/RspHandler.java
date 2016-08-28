package nio.sample.echo;

public class RspHandler {

    private byte[] rsp = null;

    public synchronized boolean handleResponse(byte[] rsp) {
        this.rsp = rsp;
        this.notify();
        return true;
    }

    public synchronized void waitForResponse() {
        while (this.rsp == null) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        System.out.println("response:" + new String(this.rsp));
    }
}
