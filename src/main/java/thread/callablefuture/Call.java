package thread.callablefuture;

import java.util.concurrent.*;
import java.util.concurrent.FutureTask;

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
