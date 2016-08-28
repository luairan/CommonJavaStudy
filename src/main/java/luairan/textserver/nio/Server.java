package luairan.textserver.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
/**
 *  nio �������
 * @author airan.lar
 *
 */
public class Server {
public static ConcurrentHashMap<SocketChannel, String> map =new ConcurrentHashMap<SocketChannel, String>();
	
	public static void main(String[] args) throws IOException {
			Selector  selector = SelectorProvider.provider().openSelector();
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.socket().setReuseAddress(true);
			serverChannel.configureBlocking(false);
			InetSocketAddress isa = new InetSocketAddress("localhost",9000);
			serverChannel.socket().bind(isa);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			while (true) {
				try {
					selector.select();
					Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
					while (selectedKeys.hasNext()) {
						SelectionKey key = selectedKeys.next();
						selectedKeys.remove();
	                    if (!key.isValid()) {
	                        continue;
	                    }
	                    if (key.isAcceptable()) {
	                        accept(key,selector);
	                    } else if (key.isReadable()) {
	                        read(key,selector);
	                    } else if (key.isWritable()) {
	                        write(key,selector);
	                    }
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	}
	
	public static void read(SelectionKey key,Selector selector) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(8192);
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
		String input = new String(readBuffer.array()).trim();
		System.out.println(input);
		map.put(socketChannel, input);
		socketChannel.register(selector, SelectionKey.OP_WRITE);
	}
    public static void write(SelectionKey key,Selector selector) throws IOException {
    	SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(8192);
		readBuffer.clear();
		String input = map.get(socketChannel);
		if(input.equals("quit")){
			readBuffer.put("ok\n".getBytes());
			readBuffer.flip();
			socketChannel.write(readBuffer);
			socketChannel.close();
		}else{
			readBuffer.put((input+"\n").getBytes());
			readBuffer.flip();
			socketChannel.write(readBuffer);
			System.out.println(9);
			socketChannel.register(selector, SelectionKey.OP_READ);
		}
		
		
    }
	public static void accept(SelectionKey key,Selector selector) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key .channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
	}
}
