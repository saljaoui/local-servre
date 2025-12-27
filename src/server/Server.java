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
import utils.Logger;

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
                        handleAccept(key);
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
            e.printStackTrace();
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
                // register client and attach a ConnectionHandler to manage I/O
                SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
                ConnectionHandler handler = new ConnectionHandler(clientChannel, clientKey);
                clientKey.attach(handler);
                // Create ConnectionHandler and attach to client's key
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        try {
            boolean readComplete = handler.read();
            if (readComplete) {
                handler.setState(ConnectionHandler.State.PROCESSING);

                try {
                    // Process the request
                    handler.processRequest();

                    // Change interest to write so selector will notify when writable
                    key.interestOps(SelectionKey.OP_WRITE);
                } catch (Exception e) {
                    Logger.error("Server", "Error processing request: " + e.getMessage(), e);
                    handler.sendErrorResponse(500, "Internal Server Error");
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }

        } catch (IOException e) {
            Logger.debug("Server", "Client disconnected: " + e.getMessage());
            try {
                channel.close();
            } catch (IOException ignore) {
            }
            key.cancel();
        }
    }

    private void handleWrite(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }
        try {
            boolean finished = handler.write();
            if (finished) {
                handler.close();
            }
        } catch (IOException e) {
            try { handler.close(); } catch (Exception ignore) {}
        }
    }
    
}
