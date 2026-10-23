package nioSample.nio.sample.echo;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class NioClient implements Runnable {

    // The host:port combination to connect to
    private InetAddress hostAddress;
    private int         port;

    // The selector we'll be monitoring
    private Selector    selector;

    // The buffer into which we'll read data when it's available
    private ByteBuffer  readBuffer     = ByteBuffer.allocate(8192);

    // A list of PendingChange instances
    private List        pendingChanges = new LinkedList();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private Map         pendingData    = new HashMap();

    // Maps a SocketChannel to a RspHandler
    private Map         rspHandlers    = Collections.synchronizedMap(new HashMap());

    public NioClient(InetAddress hostAddress, int port) throws IOException{
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    /**
     * @param data
     * @param handler
     * @throws IOException
     */
    // This client speaks raw echo: the expected response length equals request length.
    private final Map<SocketChannel, Integer> expectedBytes = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteArrayOutputStream> responses = new ConcurrentHashMap<>();

    public void send(byte[] data, RspHandler handler) throws IOException {
        Objects.requireNonNull(data,"data"); Objects.requireNonNull(handler,"handler");
        if (data.length == 0) { handler.handleResponse(new byte[0]); return; }
        synchronized (this.pendingChanges) {
            SocketChannel socket = this.initiateConnection();
            this.rspHandlers.put(socket, handler);
            expectedBytes.put(socket,data.length);
            responses.put(socket,new ByteArrayOutputStream());
            synchronized (this.pendingData) {
                List queue = new ArrayList();
                queue.add(ByteBuffer.wrap(data.clone()));
                this.pendingData.put(socket,queue);
            }
        }
        this.selector.wakeup();
    }

    private void close(SocketChannel socket, Throwable failure) throws IOException {
        SelectionKey key = socket.keyFor(selector);
        if (key != null) key.cancel();
        socket.close();
        synchronized (pendingData) { pendingData.remove(socket); }
        expectedBytes.remove(socket); responses.remove(socket);
        RspHandler handler = (RspHandler) rspHandlers.remove(socket);
        if (failure != null && handler != null) handler.handleFailure(failure);
    }

    public void run() {
        try {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Process any pending changes
                synchronized (this.pendingChanges) {
                    Iterator changes = this.pendingChanges.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest change = (ChangeRequest) changes.next();
                        switch (change.type) {
                            case ChangeRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this.selector);
                                if (key != null && key.isValid()) key.interestOps(change.ops);
                                break;
                            case ChangeRequest.REGISTER:
                                change.socket.register(this.selector, change.ops);
                                break;
                        }
                    }
                    this.pendingChanges.clear();
                }

                // Wait for an event one of the registered channels
                this.selector.select();

                // Iterate over the set of keys for which events are available
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    try {
                    if (key.isConnectable()) {
                        this.finishConnection(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                    } catch (IOException e) { close((SocketChannel) key.channel(),e); }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        } finally {
            try {
                synchronized (pendingChanges) {
                    for (Object item : pendingChanges) close(((ChangeRequest) item).socket,new IOException("Client stopped"));
                    pendingChanges.clear();
                }
                for (SelectionKey key : selector.keys()) close((SocketChannel) key.channel(),new IOException("Client stopped"));
                selector.close();
            } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            close(socketChannel,e);
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            close(socketChannel,new EOFException("Response ended before expected length"));
            return;
        }

        if (numRead == 0) return;

        // Handle the response
        this.handleResponse(socketChannel, this.readBuffer.array(), numRead);
    }

    private void handleResponse(SocketChannel socket, byte[] data, int count) throws IOException {
        ByteArrayOutputStream response = responses.get(socket);
        response.write(data,0,count);
        int expected = expectedBytes.get(socket);
        if (response.size() > expected) { close(socket,new IOException("Unexpected echo length")); return; }
        if (response.size() == expected) {
            RspHandler handler = (RspHandler) rspHandlers.get(socket);
            if (handler.handleResponse(response.toByteArray())) close(socket,null);
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);

            // Write until there's not more data ...
            while (!queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.remove(0);
            }

            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            if (!socketChannel.finishConnect()) return;
        } catch (IOException e) {
            // Cancel the channel's registration with our selector
            System.out.println(e);
            close(socketChannel,e);
            return;
        }

        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private SocketChannel initiateConnection() throws IOException {
        // Create a non-blocking socket channel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        // Kick off connection establishment
        boolean connected;
        try { connected = socketChannel.connect(new InetSocketAddress(this.hostAddress,this.port)); }
        catch (IOException e) { socketChannel.close(); throw e; }

        // Queue a channel registration since the caller is not the
        // selecting thread. As part of the registration we'll register
        // an interest in connection events. These are raised when a channel
        // is ready to complete connection establishment.
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, connected ? SelectionKey.OP_WRITE : SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }

    private Selector initSelector() throws IOException {
        // Create a new selector
        return SelectorProvider.provider().openSelector();
    }

    public static void main(String[] args) {
        try {
            NioClient client = new NioClient(InetAddress.getByName("localhost"), 9090);
            Thread t = new Thread(client);
            t.setDaemon(true);
            t.start();
            RspHandler handler = new RspHandler();
            client.send("Hello World".getBytes(), handler);
            handler.waitForResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
