package exam;

public class Thread_Daemon {
	public static void main(String[] args) {
		DaemonThread s=new DaemonThread();
		Thread t=new Thread(s);
		t.setDaemon(true);
		t.start();
//		while(true){
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			System.out.println("main");
//		}
	}
}
class DaemonThread implements Runnable{
	@Override
	public void run() {
		int i=0;
		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(Thread.currentThread().getName()+":"+(i++));
		}
	}
}