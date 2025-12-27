package server;

import config.model.WebServerConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {

    private final WebServerConfig config;
    private final Map<Integer, ServerSocketChannel> serverChannels;
    private Selector selector;

    public Server(WebServerConfig config) {
        this.config = config;
        this.serverChannels = new HashMap<>();
    }

    private void setupServers() throws IOException {
        selector = Selector.open();
        for (var srv : config.getServers()) {
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.bind(new InetSocketAddress(srv.getDefaultListen().getPort()));
            channel.configureBlocking(false);
            SelectionKey sk = channel.register(selector, SelectionKey.OP_ACCEPT);
            sk.attach(srv); // Attach server config to the key
            // serverChannels.put(srv.getDefaultListen().getPort(), channel);
        }
    }

    public void start() {

        try {
            setupServers(); // channel.close();
            System.out.println("Server running on http://localhost:" + "8080");
            while (true) {
                // wait for events
                int readySocket = selector.select();
                // step 2
                if (readySocket == 0) {
                    continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        if (key.channel() instanceof ServerSocketChannel serverChannel) {
                            SocketChannel client = serverChannel.accept();
                            // here we can check for null because in non-blocking mode, accept() returns
                            if (client == null) {
                                continue;
                            }
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                            // null if no connection is available
                        }
                        // handleAccept(key);
                    } else if (key.isReadable()) {

                        // SocketChannel client = (SocketChannel) key.channel();
                        // ByteBuffer buffer = ByteBuffer.allocate(1024);
                        // client.read(buffer);
                        // String response = "HTTP/1.1 201 OK\r\n"
                        //         + "Content-Length: 5\r\n"
                        //         + "\r\n"
                        //         + "Hello omar & simo from soufian";
                        // client.write(ByteBuffer.wrap(response.getBytes()));
                        // client.close();
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
            }
        } catch (IOException e) {

        }
    }

    private void handleAccept(SelectionKey key) {
        if (key.channel() instanceof ServerSocketChannel serverChannel) {
            try {
                var clientChannel = serverChannel.accept();
                if (clientChannel == null) {
                    return;
                }
                // Configure the client channel to be non-blocking
                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setTcpNoDelay(true);
                // Register the client channel with the selector for read operations
                // get server config attached to server socket key
                var srv = (config.model.WebServerConfig.ServerBlock) key.attachment();
                Connection conn = new Connection();
                conn.serverBlock = srv;
                clientChannel.register(selector, SelectionKey.OP_READ, conn);
                // Create ConnectionHandler and attach to client's key
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRead(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        try {

        } catch (Exception e) {
        }
        System.out.println("Server.handleRead()");
    }

    private void handleWrite(SelectionKey key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class Connection {

        java.nio.ByteBuffer readBuf = java.nio.ByteBuffer.allocate(8192);
        java.nio.ByteBuffer writeBuf;
        config.model.WebServerConfig.ServerBlock serverBlock;
    }
}
