package thread;

public class Interrupt extends Thread {

	@Override
	public void run() {
		long number = 1L;
		while(true) {
			if(isPrime(number)) {
				System.out.println("Number " + number + " is Prime");
			}
			if(isInterrupted()) {
				System.out.println("The Prime Generator has been Interrupted");
				return;
			}
			number++;
		}
	}

	private boolean isPrime(long number) { //�ж�һ�����Ƿ�������
		if(number <= 2) {
			return true;
		}
		for(long i = number-1; i > 1L; i--) {
			if(number%i == 0) {
				return false;
			}
		}
		return true;
	}
	
}
