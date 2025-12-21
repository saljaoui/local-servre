package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import config.ServerConfig;
import utils.Logger;

public class Server {
    private final ServerConfig config;
    private Selector selector;
    private Map<Integer, ServerSocketChannel> serverChannels;
    private Map<SocketChannel, ConnectionHandler> connection;
    private volatile boolean running;
    // timeout management
    private static final long CONNECTION_TIMEOUT_MS = 30000;

    private final Map<SocketChannel, Long> lastActivityTime;

    public Server(ServerConfig config) {
        this.config = config;
        this.serverChannels = new HashMap<>();
        this.connection = new HashMap<>();
        this.lastActivityTime = new HashMap<>();
        this.running = false;
    }

    // Initialize server: create selector and bind to ports
    private void init() throws IOException {
        // create server
        selector = Selector.open();
        for (int port : config.getPorts()) {
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);// no bloking
            channel.socket().bind(new InetSocketAddress(config.getHost(), port));

            channel.register(selector, SelectionKey.OP_ACCEPT);
            // System.err.println("channel" + ch.toString() + " == " + sk);
            serverChannels.put(port, channel);

        }
        Logger.info("Server", "Server initialized on ports: " + config.getPorts());
    }

    public void start() throws IOException {
        init();
        running = true;
        while (running) {
            // step 1
            int readySocket = selector.select(CONNECTION_TIMEOUT_MS);
            // step 2
            // some code here

            // step 3
            var selectedKeys = selector.selectedKeys();
            var iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid())
                    continue;
                try {
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);

                    }
                } catch (Exception e) {
                }

            }
        }
        Logger.info("server", "Server started. Listening for connections...");
    }
//Handle readable channel (incoming data)
    private void handleRead(SelectionKey key) throws IOException {
        //  Auto-generated method stub
        ConnectionHandler handler= (ConnectionHandler) key.attachment();
        SocketChannel channel=(SocketChannel ) key.channel();
        // Read Data
          boolean readComplet=handler.read();
        lastActivityTime.put(channel, System.currentTimeMillis());
        if (readComplet){
            key.interestOps(SelectionKey.OP_WRITE);
        }
     }

    private void handleAccept(SelectionKey key) throws IOException {
        if (key.channel() instanceof ServerSocketChannel channel) {
            var client = channel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            // Create ConnectionHandler and attach to key
            ConnectionHandler handler = new ConnectionHandler(client, key);
            connection.put(client, handler);
            Logger.info("Client", "New connection accepted from: " + client.getRemoteAddress());
        } else {
            throw new RuntimeException("Unknown Channel");
        }
    }

    private void handleWrite(SelectionKey key) {
     }

}