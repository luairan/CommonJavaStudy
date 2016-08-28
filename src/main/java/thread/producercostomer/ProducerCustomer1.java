package thread.producercostomer;

public class ProducerCustomer1 {
	public static int limit = 10;
	public static int have = 0;
	public static int start = 0;
	Task[] tasklist = new Task[limit];
	class Producer implements Runnable {
		@Override
		public void run() {
			while (true) {
				synchronized (tasklist) {
					if (ProducerCustomer1.have < ProducerCustomer1.limit) {
						Task temp = new Task(
								"mytest"
										+ (ProducerCustomer1.start + ProducerCustomer1.have)
										% ProducerCustomer1.limit+"\t"+Thread.currentThread().getId());
						
						tasklist[(ProducerCustomer1.start + ProducerCustomer1.have)
								% ProducerCustomer1.limit] = temp;
						ProducerCustomer1.have++;
						tasklist.notifyAll();
					} else {
						try {
							tasklist.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}
	}

	class Customer implements Runnable {
		@Override
		public void run() {
			while (true) {
				Task temp = null;
				synchronized (tasklist) {
					if (ProducerCustomer1.have > 0) {
						temp = tasklist[ProducerCustomer1.start];
						ProducerCustomer1.start = (ProducerCustomer1.start + 1)
								% ProducerCustomer1.limit;
						ProducerCustomer1.have--;
						System.out.println(temp.taskname + ":"
								+ Thread.currentThread().getId());
						tasklist.notifyAll();
					} else {
						try {
							tasklist.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
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
