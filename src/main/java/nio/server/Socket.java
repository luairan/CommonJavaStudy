package server;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socket {
	public static void main(String[] args) throws Exception {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
		AsynchronousServerSocketChannel socket = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
		socket.bind(new InetSocketAddress(9999));
		socket.accept(socket, new Handler());
		//因为AIO不会阻塞调用进程，因此必须在主进程阻塞，才能保持进程存活。  
//        try {  
//            Thread.sleep(Integer.MAX_VALUE);  
//        } catch (InterruptedException e) {  
//            e.printStackTrace();  
//        }  
	}
}
