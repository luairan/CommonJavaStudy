package nio.asyn.multithread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class OperationClient {

    // Charset and decoder for US-ASCII
    private static Charset        charset = Charset.forName("US-ASCII");

    // Direct byte buffer for reading
    private static ByteBuffer     dbuf    = ByteBuffer.allocateDirect(1024);

    // Ask the given host what time it is
    //
    private static void query(String host, int port) throws IOException {
        byte inBuffer[] = new byte[100];
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(host), port);
        SocketChannel sc = null;
        while (true) {
            try {
                System.in.read(inBuffer);
                sc = SocketChannel.open();
                sc.connect(isa);
                dbuf.clear();
                dbuf.put(inBuffer);
                dbuf.flip();
                sc.write(dbuf);
                dbuf.clear();

            } finally {
                // Make sure we close the channel (and hence the socket)
                if (sc != null) sc.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        query("localhost", 8090);//A+B
//        query("localhost", 9090);//A*B
    }
}
