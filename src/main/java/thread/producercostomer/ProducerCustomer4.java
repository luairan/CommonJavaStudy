package thread.producercostomer;

import java.util.concurrent.Semaphore;

public class ProducerCustomer4 {
	Semaphore mutex = new Semaphore(1);// ������
	Semaphore notfull = new Semaphore(10);
	Semaphore notempty = new Semaphore(0);
	String[] array = new String[10];
	int putptr, takeptr, count = 0;
	class Producer implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					notfull.acquire();
					mutex.acquire();
					if (putptr == array.length)
						putptr = 0;
					String temp = new String("temp" + ":" + putptr + ":"
							+ Thread.currentThread().getId());
					array[putptr] = temp;
					putptr++;
					count++;
					mutex.release();
					notempty.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class Consumer implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					notempty.acquire();
					mutex.acquire();
					if (takeptr == array.length)
						takeptr = 0;
					System.out.println(array[takeptr] + ":" + takeptr + ":"
							+ Thread.currentThread().getId());
					takeptr++;
					count--;
					mutex.release();
					notfull.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
