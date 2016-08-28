package threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class ThreadPool{
	
	public static void main(String[] args) {
		ExecutorService poll=Executors.newSingleThreadExecutor();
		ExecutorService poll1=Executors.newFixedThreadPool(2);
	}
	
}