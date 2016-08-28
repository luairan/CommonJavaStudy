package luairan.timeserver.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * nio �������
 * 
 * @author airan.lar
 *
 */
public class UnBlockSingleServer {
	ServerSocketChannel serverSocketChannel;
	Selector selector;

	public UnBlockSingleServer() throws IOException {
		
		 selector = Selector.open();
		
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().setReuseAddress(true);
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().bind(new InetSocketAddress(9000));
		System.out.println("Server open");
	}

	public void service() throws IOException {
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		while (selector.select()>0) {
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey>  iter  = readyKeys.iterator();
			while(iter.hasNext()){
				SelectionKey key = null; 
				  key = iter.next();
				  iter.remove();
				  if (!key.isValid()) {
                      continue;
                  }
                  if (key.isAcceptable()) {
                	  accept(key, selector);
                  } else if (key.isReadable()) {
                	  read(key, selector);
                  } else if (key.isWritable()) {
                    write(key, selector);
                  }
			}
		}
	}
	
	public  void read(SelectionKey key,Selector selector) throws IOException {
		
		ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(32);
		readBuffer.clear();
		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		readBuffer.flip();
		byteBuffer.limit(byteBuffer.capacity());
		byteBuffer.put(readBuffer);
		byteBuffer.flip();
	}
    public  void write(SelectionKey key,Selector selector) throws IOException {
    	
    	String t = new String("");
    	
    	ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
    	SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(8192);
		readBuffer.clear();
		
		String input  = decode(byteBuffer);
		
		
		System.out.println(input);
		System.out.println(input.equals(t));
		System.out.println(input.equals(""));
		System.out.println(input.equals("\n"));
		System.out.println(input.equals("\r\n"));
		if(input.equals("")) return ;
		if(input.indexOf("\r\n")==-1) return ;
		
		
		
		
		String out = input.substring(0, input.indexOf("\r\n")+1);
		
		if(out.equals("quit")){
			readBuffer.put("ok\n".getBytes());
			byteBuffer.position(readBuffer.limit());
			readBuffer.flip();
			socketChannel.write(readBuffer);
			socketChannel.close();
		}else{
			readBuffer.put((out+"\n").getBytes());
			byteBuffer.position(readBuffer.limit());
			readBuffer.flip();
			socketChannel.write(readBuffer);
			System.out.println(9);
			
			
		//	socketChannel.register(selector, SelectionKey.OP_READ);
		}
		
		byteBuffer.compact();
		
    }
	public  void accept(SelectionKey key,Selector selector) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key .channel();
  		SocketChannel socketChannel = serverSocketChannel.accept();
  		socketChannel.configureBlocking(false);
  		socketChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE,ByteBuffer.allocate(1024));
	}
	
	public String decode(ByteBuffer buffer){
		Charset charset =Charset.forName("UTF-8");
		CharBuffer bufferc = charset.decode(buffer);
		return bufferc.toString();
	}

	public static void main(String[] args) throws IOException {
		new UnBlockSingleServer().service();
	}

}

