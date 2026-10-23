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

    private boolean isPrime(long number) {
        if (number < 2) return false;
        if (number == 2) return true;
        if (number % 2 == 0) return false;
        for (long i = 3; i <= number / i; i += 2) {
            if (isInterrupted() || number % i == 0) return false;
        }
        return true;
    }
}
