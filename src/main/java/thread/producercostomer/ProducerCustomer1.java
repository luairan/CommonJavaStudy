package thread.producercostomer;

public class ProducerCustomer1 {
    private final int limit = 10;
    private int have;
    private int start;
    private final Task[] tasklist = new Task[limit];

    public void put(Task task) throws InterruptedException {
        synchronized (tasklist) {
            while (have == limit) tasklist.wait();
            tasklist[(start + have) % limit] = task;
            have++;
            tasklist.notifyAll();
        }
    }

    public Task take() throws InterruptedException {
        synchronized (tasklist) {
            while (have == 0) tasklist.wait();
            Task task = tasklist[start];
            tasklist[start] = null;
            start = (start + 1) % limit;
            have--;
            tasklist.notifyAll();
            return task;
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
		ProducerCustomer1 p = new ProducerCustomer1();
		Producer producer = p.new Producer();
		Customer customer = p.new Customer();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(producer).start();
		new Thread(customer).start();
		new Thread(customer).start();
		new Thread(customer).start();
	}
}
