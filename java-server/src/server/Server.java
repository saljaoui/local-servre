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
    private final Map<Integer, ServerSocketChannel> serverChannels;
    private final Map<SocketChannel, ConnectionHandler> connection;
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
    /*
     * bind:
     * Binds a server socket to a specific port number on the local machine.
     * This is how the server starts listening for incoming connections.
     *
     * accept:
     * Accepts an incoming client connection.
     * When a client connects to the bound port, accept() creates a SocketChannel
     * to communicate with that client.
     *
     * register:
     * Registers a Channel (e.g., ServerSocketChannel or SocketChannel)
     * with a Selector, telling the Selector which events to monitor.
     *
     * selector:
     * A Selector allows a single thread to monitor multiple channels
     * (non-blocking I/O) for different events like accept, read, write, or connect.
     *
     * SelectionKey.OP_ACCEPT:
     * Indicates that a ServerSocketChannel is ready to accept a new client
     * connection.
     *
     * SelectionKey.OP_READ:
     * Indicates that a SocketChannel has data available to be read.
     *
     * SelectionKey.OP_WRITE:
     * Indicates that a SocketChannel is ready to write data without blocking.
     *
     * SelectionKey.OP_CONNECT:
     * Indicates that a SocketChannel has finished connecting to a remote server.
     */

    private void init() throws IOException {
        // Create a selector for multiplexing I/O operations
        selector = Selector.open();
        for (int port : config.getPorts()) {
            ServerSocketChannel channel = ServerSocketChannel.open();
            // Configure channel for non-blocking mode
            channel.configureBlocking(false);// no bloking
            channel.socket().bind(new InetSocketAddress(config.getHost(), port));
            // Register channel with selector for specific operations
            Logger.info("Server", "Server started on port : " + port);

            SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
            // System.err.println("channel" + ch.toString() + " == " + sk);
            serverChannels.put(port, channel);
            key.attach(channel);

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
            if (readySocket == 0)
                continue;
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
                        System.out.println("test acceptable");
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        System.out.println("test readable");

                        handleRead(key);
                    } else if (key.isWritable()) {

                        handleWrite(key);

                    }
                } catch (Exception e) {
                    Logger.error("Server", "Error handling key: " + e.getMessage(), e);
                    cleanupConnection(key, (SocketChannel) key.channel());
                }

            }
        }
        Logger.info("server", "Server started. Listening for connections...");
    }

    // Handle readable channel (incoming data)
    private void handleRead(SelectionKey key) throws IOException {
        System.out.println("Server.handleRead(**-*-*---*)");
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        lastActivityTime.put(channel, System.currentTimeMillis());

        try {
            // Read data from the channel
            boolean readComplete = handler.read();

            if (readComplete) {
                // Update state to PROCESSING
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
            // Client disconnected or error reading
            Logger.debug("Server", "Client disconnected: " + e.getMessage());
            cleanupConnection(key, channel);
        } catch (Exception e) {
            Logger.error("Server", "Unexpected error: " + e.getMessage(), e);
            cleanupConnection(key, channel);
        }
    }

    private void cleanupConnection(SelectionKey key, SocketChannel channel) {
        try {
            if (key != null) {
                key.cancel();
            }
            if (channel != null) {
                lastActivityTime.remove(channel);
                channel.close();
            }
        } catch (IOException e) {
            Logger.error("Server", "Error cleaning up connection: " + e.getMessage());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        if (key.channel() instanceof ServerSocketChannel channel) {
            var client = channel.accept();
            if (client == null) {
                return;
            }
            client.configureBlocking(false);
            //
            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
            // Create ConnectionHandler and attach to client's key
            ConnectionHandler handler = new ConnectionHandler(client, clientKey);
            Logger.info("Client", "New connection from: " + handler.getRemoteAddress());
            // Attach handler to the key for easy access
            clientKey.attach(handler);

            // Store the connection and its activity time
            connection.put(client, handler);
            lastActivityTime.put(client, System.currentTimeMillis());
            Logger.info("Client", "New connection accepted from: " + client.getRemoteAddress());
        } else {
            throw new RuntimeException("Unknown Channel");
        }
    }

    private void handleWrite(SelectionKey key) {

        ConnectionHandler handler = (ConnectionHandler) key.attachment();

        if (handler == null) {
            return;
        }
        try {
            // System.out.println("test writable"+handler.getRemoteAddress());
            handler.write();
        } catch (IOException e) {
            Logger.error("Server", "Error writing to client", e);
            try {
                key.channel().close();
            } catch (IOException ignore) {
            }
            key.cancel();
            connection.remove((SocketChannel) key.channel());
        }
    }
}