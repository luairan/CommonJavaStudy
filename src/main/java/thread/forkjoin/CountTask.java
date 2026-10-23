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
    private final int start;
    private final int end;

    public CountTask(int start, int end) {
        if (start > end) throw new IllegalArgumentException("start must not exceed end");
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        // A subrange can exceed int even when positive and negative halves cancel.
        // Only the final result is constrained by this class's Integer return type.
        return Math.toIntExact(new SumRange(start,end).compute());
    }

    private static final class SumRange extends RecursiveTask<Long> {
        private static final long serialVersionUID = 1L;
        private static final int THRESHOLD = 4096;
        private final int start;
        private final int end;

        SumRange(int start, int end) { this.start = start; this.end = end; }

        @Override
        protected Long compute() {
            if ((long) end - start <= THRESHOLD) {
                long sum = 0;
                for (long value = start; value <= end; value++) sum += value;
                return sum;
            }
            int middle = (int) (start + ((long) end - start) / 2);
            SumRange left = new SumRange(start,middle);
            SumRange right = new SumRange(middle+1,end);
            left.fork();
            long rightSum = right.compute();
            return left.join() + rightSum;
        }
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
