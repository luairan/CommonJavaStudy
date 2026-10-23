package thread.callablefuture;

import java.util.concurrent.*;

public class FutureTask{
public static void main(String[] args) {
	ExecutorService s=Executors.newSingleThreadExecutor();
	@SuppressWarnings("rawtypes")
	Future future = s.submit(new Callable<String>() {
		public String call(){
			return "luarian s";
		}
	});
	try {
		System.out.println(future.get());

	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		Thread.currentThread().interrupt();
	} catch (ExecutionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
    } finally {
        s.shutdownNow();
    }
}
}
