package thread.exam;

public class Thread_interrput {
	public static void main(String[] args) {
		S s=new S();
		Thread t=new Thread(s);
		t.start();
		t.interrupt();
	}
}

class S implements Runnable{
	public volatile boolean stop = false;
	@Override
	public void run() {
		while(!stop){
			 System.out.println(Thread.currentThread().getName() + " is running..");
			 try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("insss");
				  Thread.currentThread().interrupt();
				e.printStackTrace();
			}
		}
	}
	
}
