package thread.producercostomer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerCustomer2 {
    private final int limit = 10;
    private int have;
    private int start;
    private final Task[] tasklist = new Task[limit];

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public void put(Task task) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (have == limit) notFull.await();
            tasklist[(start + have) % limit] = task;
            have++;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Task take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (have == 0) notEmpty.await();
            Task task = tasklist[start];
            tasklist[start] = null;
            start = (start + 1) % limit;
            have--;
            notFull.signalAll();
            return task;
        } finally {
            lock.unlock();
        }
    }

    class Producer implements Runnable {
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    put(new Task("mytest:" + Thread.currentThread().getId()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Customer implements Runnable {
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Task task = take();
                    System.out.println(task.taskname + ":" + Thread.currentThread().getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class Task {
        public final String taskname;
        public Task(String taskname) { this.taskname = taskname; }
    }
	public static void main(String[] args) {
		ProducerCustomer2 p = new ProducerCustomer2();
		Producer producer = p.new Producer();
		Customer customer = p.new Customer();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(producer).start();
		//new Thread(producer).start();
		new Thread(customer).start();
		new Thread(customer).start();
		new Thread(customer).start();
	}
}

