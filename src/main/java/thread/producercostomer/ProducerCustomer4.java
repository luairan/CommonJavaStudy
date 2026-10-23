package thread.producercostomer;

import java.util.concurrent.Semaphore;

public class ProducerCustomer4 {
    final Semaphore mutex = new Semaphore(1);
    final Semaphore notfull = new Semaphore(10);
    final Semaphore notempty = new Semaphore(0);
    final String[] array = new String[10];
    int putptr, takeptr, count;

    public void put(String value) throws InterruptedException {
        notfull.acquire();
        boolean locked = false;
        boolean committed = false;
        try {
            mutex.acquire();
            locked = true;
            array[putptr] = value;
            putptr = (putptr + 1) % array.length;
            count++;
            committed = true;
        } finally {
            if (locked) mutex.release();
            if (committed) notempty.release();
            else notfull.release();
        }
    }

    public String take() throws InterruptedException {
        notempty.acquire();
        boolean locked = false;
        boolean committed = false;
        try {
            mutex.acquire();
            locked = true;
            String value = array[takeptr];
            array[takeptr] = null;
            takeptr = (takeptr + 1) % array.length;
            count--;
            committed = true;
            return value;
        } finally {
            if (locked) mutex.release();
            if (committed) notfull.release();
            else notempty.release();
        }
    }

    class Producer implements Runnable {
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    put("temp:" + Thread.currentThread().getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Consumer implements Runnable {
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println(take() + ":" + Thread.currentThread().getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

	public static void main(String[] args) {
		ProducerCustomer4 s = new ProducerCustomer4();
		Producer producer = s.new Producer();
		Consumer consumer = s.new Consumer();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(consumer).start();
		new Thread(consumer).start();
	}
}
