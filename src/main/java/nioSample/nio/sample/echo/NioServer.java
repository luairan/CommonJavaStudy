package nioSample.nio.sample.echo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class NioServer implements Runnable, AutoCloseable {

    // The host:port combination to listen on
    private InetAddress         hostAddress;
    private int                 port;

    // The channel on which we'll accept connections
    private ServerSocketChannel serverChannel;

    // The selector we'll be monitoring
    private Selector            selector;

    // The buffer into which we'll read data when it's available
    private ByteBuffer          readBuffer     = ByteBuffer.allocate(8192);

    private EchoWorker          worker;

    // A list of PendingChange instances
    private List                pendingChanges = new LinkedList();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private Map                 pendingData    = new HashMap();

    public NioServer(InetAddress hostAddress, int port, EchoWorker worker) throws IOException{
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
        this.worker = worker;
    }

    private volatile boolean stopped;

    private final Map<SocketChannel,Integer> inFlight = new HashMap<>();
    private final Set<SocketChannel> inputClosed = new HashSet<>();

    public void send(SocketChannel socket, byte[] data) {
        synchronized (this.pendingChanges) {
            if (stopped || !socket.isOpen()) return;
            // Indicate we want the interest ops set changed
            this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            // And queue the data we want written
            synchronized (this.pendingData) {
                if (!socket.isOpen()) return;
                inFlight.computeIfPresent(socket,(channel,count) -> count - 1);
                List queue = (List) this.pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList();
                    this.pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }

        // Finally, wake up our selecting thread so it can make the required changes
        this.selector.wakeup();
    }

    public void run() {
        try {
        while (!stopped && !Thread.currentThread().isInterrupted()) {
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
                        }
                    }
                    this.pendingChanges.clear();
                }

                // Wait for an event one of the registered channels
                this.selector.select();
                synchronized (pendingChanges) {
                if (stopped) break;

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
                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                    } catch (IOException e) {
                        if (key.channel() instanceof SocketChannel) close((SocketChannel) key.channel());
                        else throw e;
                    }
                }
                }
            } catch (Exception e) {
                if (!stopped) e.printStackTrace();
            }
        }
        } finally {
            try { close(); }
            catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }
    }

    public int getPort() throws IOException {
        return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
    }

    @Override
    public void close() throws IOException {
        synchronized (pendingChanges) {
            stopped = true;
            selector.wakeup();
            if (selector.isOpen()) {
                try {
                    for (SelectionKey key : selector.keys()) {
                        if (key.channel() instanceof SocketChannel) close((SocketChannel) key.channel());
                        else key.channel().close();
                    }
                } finally { selector.close(); }
            }
            serverChannel.close();
            pendingChanges.clear();
            synchronized (pendingData) { pendingData.clear(); inFlight.clear(); inputClosed.clear(); }
        }
    }

    private void close(SocketChannel socket) throws IOException {
        SelectionKey key = socket.keyFor(selector);
        if (key != null) key.cancel();
        socket.close();
        synchronized (pendingData) { pendingData.remove(socket); inFlight.remove(socket); inputClosed.remove(socket); }
    }

    private void accept(SelectionKey key) throws IOException {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel == null) return;
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        socketChannel.register(this.selector, SelectionKey.OP_READ);
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
            close(socketChannel);
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            synchronized (pendingData) {
                inputClosed.add(socketChannel);
                List queued = (List) pendingData.get(socketChannel);
                if (inFlight.getOrDefault(socketChannel,0) == 0 && (queued == null || queued.isEmpty())) close(socketChannel);
                else key.interestOps(queued == null || queued.isEmpty() ? 0 : SelectionKey.OP_WRITE);
            }
            return;
        }

        if (numRead == 0) return;

        // Hand the data off to our worker thread
        synchronized (pendingData) { inFlight.merge(socketChannel,1,Integer::sum); }
        this.worker.processData(this, socketChannel, this.readBuffer.array(), numRead);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);

            if (queue == null) { key.interestOps(SelectionKey.OP_READ); return; }

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
                this.pendingData.remove(socketChannel);
                if (inputClosed.contains(socketChannel)) {
                    if (inFlight.getOrDefault(socketChannel,0) == 0) close(socketChannel);
                    else key.interestOps(0);
                } else key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.setOption(java.net.StandardSocketOptions.SO_REUSEADDR,true);
            serverChannel.bind(new InetSocketAddress(hostAddress,port));
            serverChannel.register(socketSelector,SelectionKey.OP_ACCEPT);
            return socketSelector;
        } catch (IOException | RuntimeException e) {
            if (serverChannel != null) serverChannel.close();
            socketSelector.close();
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            EchoWorker worker = new EchoWorker();
            NioServer server = new NioServer(null,args.length == 0 ? 9090 : Integer.parseInt(args[0]),worker);
            Thread thread = new Thread(worker); thread.start();
            try { server.run(); } finally { thread.interrupt(); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
