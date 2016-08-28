

public class InterruptMain {
	public static void main(String[] args) {
		Thread task = new Interrupt();
		task.start();
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		task.interrupt();
	}
}
