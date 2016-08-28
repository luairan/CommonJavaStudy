package luairan.timeserver.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socket {
	public static void main(String[] args) throws Exception {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
		AsynchronousServerSocketChannel socket = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
		socket.bind(new InetSocketAddress(9000));
		socket.accept(socket, new Handler());
		//��ΪAIO�����������ý��̣���˱��������������������ܱ��ֽ��̴�  
        try {  
            Thread.sleep(Integer.MAX_VALUE);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }  
	}
}
class Handler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>{
	ByteBuffer echoBuffer = ByteBuffer.allocate(1024*5);
	CharsetDecoder decoder=Charset.forName("UTF-8").newDecoder();
	@Override
	public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
		try {
			
			while(true){
				echoBuffer.clear();
				result.read(echoBuffer).get();
				echoBuffer.flip();
				String input = new String(echoBuffer.array(),0,echoBuffer.limit());
				echoBuffer.clear();
				if(input.equals("quit\n")){
					echoBuffer.put("ok\n".getBytes());
					echoBuffer.flip();
					result.write(echoBuffer).get();
					echoBuffer.flip();
					break;
				}else{
					echoBuffer.put(("server: "+((InetSocketAddress)result.getLocalAddress()).getAddress()+":"+((InetSocketAddress)result.getLocalAddress()).getPort()+"\t input \t :"+input).getBytes());
					echoBuffer.flip();
					result.write(echoBuffer).get();
					echoBuffer.flip();
				}
			}
			
		} catch (InterruptedException| ExecutionException | IOException e) {
			System.out.println(e.toString());
		} finally {
			try {
				result.close();
				attachment.accept(attachment, this);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	@Override
	public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
	}
}
