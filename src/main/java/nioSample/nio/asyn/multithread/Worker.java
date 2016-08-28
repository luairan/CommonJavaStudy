package nio.asyn.multithread;

import java.util.LinkedList;
import java.util.List;

public class Worker implements Runnable {

	private List<EventData> queue = new LinkedList<EventData>();

	public void processData(EventData event) {
		synchronized (queue) {
			queue.add(event);
			queue.notify();
		}
	}

	public void run() {
		EventData eventData;
		while (true) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						System.out
								.println("No data to process, worker thread will sleep.");
						queue.wait();
					} catch (InterruptedException e) {
					}
					eventData = (EventData) queue.remove(0);
					System.out.println("Client event is:"
							+ eventData.getMessage());
				}

			}
		}
	}
}
