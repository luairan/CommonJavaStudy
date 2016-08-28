package nio.asyn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class OperationServer implements Runnable {

    private int                 port1            = 8090;
    private int                 port2            = 9090;
    private Selector            selector;
    private ServerSocketChannel serverChannel1;
    private ByteBuffer          readBuffer       = ByteBuffer.allocate(8192);
    private ServerSocketChannel serverChannel2;
    private SocketChannel       socketChannel1;
    private SocketChannel       socketChannel2;
    private AddProcessor    client1Processor = new AddProcessor();
    private MultiProcessor    client2Processor = new MultiProcessor();

    public OperationServer(){
        initSelector();
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.selector.select();
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        if (serverSocketChannel.equals(serverChannel1)) {
            socketChannel1 = serverSocketChannel.accept();
            socketChannel1.configureBlocking(false);
            socketChannel1.register(this.selector, SelectionKey.OP_READ);
        } else {
            socketChannel2 = serverSocketChannel.accept();
            socketChannel2.configureBlocking(false);
            socketChannel2.register(this.selector, SelectionKey.OP_READ);
        }

    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
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
        if (socketChannel.equals(socketChannel1)) {
            client1Processor.process(input);
        } else {
            client2Processor.process(input);
        }
    }

    public void write(SelectionKey key) {

    }

    /**
     * ×¢²áÊÂ¼þµ½selector£»
     */
    public void initSelector() {
        try {
            selector = SelectorProvider.provider().openSelector();
            this.serverChannel1 = ServerSocketChannel.open();
            serverChannel1.configureBlocking(false);
            InetSocketAddress isa = new InetSocketAddress("localhost", this.port1);
            serverChannel1.socket().bind(isa);
            serverChannel1.register(selector, SelectionKey.OP_ACCEPT);

            this.serverChannel2 = ServerSocketChannel.open();
            serverChannel2.configureBlocking(false);
            InetSocketAddress isa2 = new InetSocketAddress("localhost", this.port2);
            serverChannel2.socket().bind(isa2);
            serverChannel2.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        OperationServer server = new OperationServer();
        Thread t = new Thread(server);
        t.start();
    }

}
