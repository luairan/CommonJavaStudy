package thread.producercostomer;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class ProducerCustomer2 {

	public static int limit = 10;
	public static int have = 0;
	public static int start = 0;
	Task[] tasklist = new Task[limit];
	private final Lock lock = new ReentrantLock();
	private final Condition full = lock.newCondition();
	private final Condition empty = lock.newCondition();

	class Producer implements Runnable {
		@Override
		public void run() {
			while (true) {
				lock.lock();
				if (ProducerCustomer2.have < ProducerCustomer2.limit) {
					Task temp = new Task(
							"mytest"
									+ (ProducerCustomer2.start + ProducerCustomer2.have)
									% ProducerCustomer2.limit + "\t"
									+ Thread.currentThread().getName());
					tasklist[(ProducerCustomer2.start + ProducerCustomer2.have)
							% ProducerCustomer2.limit] = temp;
					ProducerCustomer2.have++;
					empty.signalAll();
				} else {
					try {
						full.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				lock.unlock();
			}

		}

	}

	class Customer implements Runnable {
		@Override
		public void run() {
			while (true) {
				Task temp = null;
				lock.lock();
				if (ProducerCustomer2.have > 0) {
					temp = tasklist[ProducerCustomer2.start];
					ProducerCustomer2.start = (ProducerCustomer2.start + 1)
							% ProducerCustomer2.limit;
					ProducerCustomer2.have--;
					System.out.println(temp.taskname + ":"
							+ Thread.currentThread().getName());
					full.signalAll();
				} else {
					try {
						empty.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				lock.unlock();
			}
		}
	}

	class Task {
		public String taskname;

		public Task(String taskname) {
			this.taskname = taskname;
		}
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

