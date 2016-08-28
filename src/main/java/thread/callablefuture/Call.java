package callablefuture;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Call {
	public static void main(String[] args) {
		ArrayBlockingQueue<Runnable> a=new ArrayBlockingQueue<>(3);
		ExecutorService executorService = new ThreadPoolExecutor(1, 3, 3, TimeUnit.MILLISECONDS, a);
		FutureTask<String> funture =new FutureTask<String>(new Callable<String>() {
			public String call(){
				System.out.println("1"+Thread.currentThread().getName());
				return "test";
			}
		});
		executorService.execute(funture);
		try {
			String r =funture.get();
			System.out.println(r);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
