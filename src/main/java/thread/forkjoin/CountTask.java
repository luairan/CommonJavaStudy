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
	private final int start;
	private final int end;

	public CountTask(int start, int end) {
        if (start > end) throw new IllegalArgumentException("start must not exceed end");
		this.start = start;
		this.end = end;
	}

	@Override
	protected Integer compute() {
		int sum = 0;
		boolean canCompute = ((long) end - start) <= THRESHOLD;
		if (canCompute) {
			for (long i = start; i <= end; i++) {
				sum = Math.addExact(sum, Math.toIntExact(i));
			}
		} else {
			int middle = (int) (start + ((long) end - start) / 2);
			CountTask left = new CountTask(start, middle);
			CountTask right = new CountTask(middle + 1, end);
			left.fork();
            int rightr = right.compute();
            int leftr = left.join();
            sum = Math.addExact(leftr, rightr);
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
            Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			e.printStackTrace();
        } finally {
            forkJoinPool.shutdownNow();
        }
	}
}
