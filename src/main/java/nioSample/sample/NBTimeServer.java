package nioSample.sample;

/*
 * @(#)NBTimeServer.java 1.4 01/12/13 A non blocking Internet time server implemented using the New I/O (NIO) facilities
 * added to J2SE v 1.4. Copyright 2001-2002 Oracle and/or its affiliates. All rights reserved. Redistribution and use in
 * source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. -Redistribution in binary form must reproduct the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of
 * Oracle and/or its affiliates. or the names of contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission. This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR RELATING TO USE,
 * MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. You acknowledge that Software is not designed,
 * licensed or intended for use in the design, construction, operation or maintenance of any nuclear facility.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

// Listen on a port for connections and write back the current time.
public class NBTimeServer {

    private static final int DEFAULT_TIME_PORT = 8900;

    // Constructor with no arguments creates a time server on default port.
    public NBTimeServer() throws Exception{
        acceptConnections(this.DEFAULT_TIME_PORT);
    }

    // Constructor with port argument creates a time server on specified port.
    public NBTimeServer(int port) throws Exception{
        acceptConnections(port);
    }

    // Accept connections for current time. Lazy Exception thrown.
    private static void acceptConnections(int port) throws Exception {
        try (Selector selector = Selector.open(); ServerSocketChannel listener = ServerSocketChannel.open()) {
            listener.configureBlocking(false);
            listener.bind(new InetSocketAddress("localhost",port));
            listener.register(selector,SelectionKey.OP_ACCEPT);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next(); keys.remove();
                        if (!key.isValid()) continue;
                        try {
                            if (key.isAcceptable()) {
                                java.nio.channels.SocketChannel client = listener.accept();
                                if (client == null) continue;
                                client.configureBlocking(false);
                                client.register(selector,SelectionKey.OP_WRITE,
                                        java.nio.charset.StandardCharsets.US_ASCII.encode(new Date() + "\r\n"));
                            } else if (key.isWritable()) {
                                java.nio.ByteBuffer response = (java.nio.ByteBuffer) key.attachment();
                                ((java.nio.channels.SocketChannel) key.channel()).write(response);
                                if (!response.hasRemaining()) { key.cancel(); key.channel().close(); }
                            }
                        } catch (IOException e) { key.cancel(); key.channel().close(); }
                    }
                }
            } finally { for (SelectionKey key : selector.keys()) key.channel().close(); }
        }
    }

    // Entry point.
    public static void main(String[] args) {
        // Parse command line arguments and
        // create a new time server (no arguments yet)
        try {
            NBTimeServer nbt = new NBTimeServer(args.length == 0 ? DEFAULT_TIME_PORT : Integer.parseInt(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
