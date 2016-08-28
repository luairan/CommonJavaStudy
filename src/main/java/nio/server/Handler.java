package server;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.ExecutionException;
public class Handler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>{
	ByteBuffer echoBuffer = ByteBuffer.allocateDirect(1024);
	CharsetDecoder decoder=Charset.forName("UTF-8").newDecoder();
	@Override
	public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
		System.out.println("waiting ....");
		try {
			echoBuffer.clear();
			result.read(echoBuffer).get();
			echoBuffer.flip();
			result.write(echoBuffer);
			echoBuffer.flip();
			 try {
				System.out.println("Echoed '" + new String(decoder.decode(echoBuffer).array()));
			} catch (CharacterCodingException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException| ExecutionException e) {
			System.out.println(e.toString());
		} finally {
			try {
				result.close();
				attachment.accept(attachment, this);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
		System.out.println("done...");
	}

	@Override
	public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
	}

}
