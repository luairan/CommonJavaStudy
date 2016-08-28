package producercostomer;

import java.util.concurrent.LinkedBlockingQueue;

public class ProducerCustomer3 {
	public static int limit = 10;
	public static int have = 0;
	public static int start = 0;
	LinkedBlockingQueue<Tasks> queue = new LinkedBlockingQueue<Tasks>(limit);
	class Customer implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					Tasks ss=queue.take();
					System.out.println(ss.taskname+":"+Thread.currentThread().getId());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class Producer implements Runnable {
		@Override
		public void run() {
			while (true) {
				Tasks o = new Tasks("test"+Thread.currentThread().getId());
	             try {
					queue.put(o);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

	class Tasks {
		public String taskname;

		public Tasks(String taskname) {
			this.taskname = taskname;
		}
	}
	public static void main(String[] args) {
		ProducerCustomer3 p = new ProducerCustomer3();
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
