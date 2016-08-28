package thread.forkjoin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

public class CountTask extends RecursiveTask<Integer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int THRESHOLD = 2;
	private int start;
	private int end;

	public CountTask(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	protected Integer compute() {
		int sum = 0;
		boolean canCompute = (end - start) <= THRESHOLD;
		if (canCompute) {
			for (int i = start; i <= end; i++) {
				sum += i;
			}
		} else {
			int middle = (end + start) / 2;
			CountTask left = new CountTask(start, middle);
			CountTask right = new CountTask(middle + 1, end);
			left.fork();
			right.fork();
			int leftr = left.join();
			int rightr = right.join();
			sum = leftr + rightr;
		}
		return sum;
	}
	public static void main(String[] args) {
		ForkJoinPool forkJoinPool =new ForkJoinPool();
		CountTask task=new CountTask(1, 4);
		Future<Integer> result =forkJoinPool.submit(task);
		try {
			System.out.println(result.get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
