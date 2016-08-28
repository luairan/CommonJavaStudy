package callablefuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		s.shutdown();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ExecutionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
