package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import config.ServerConfig;
import utils.Logger;

public class EventLoop {
    private static final String TAG = "EventLoop";
    private static final long TIMEOUT_CHECK_INTERVAL = 1000; // Check timeouts every second
    
    private final ServerConfig config;
    private final Selector selector;
    private final TimeoutManager timeoutManager;
    private volatile boolean running;
    private long lastTimeoutCheck;
    
    public EventLoop(ServerConfig config) throws IOException {
        this.config = config;
        this.selector = Selector.open();
        this.timeoutManager = new TimeoutManager(config.getTimeout());
        this.running = false;
        this.lastTimeoutCheck = System.currentTimeMillis();
    }
    
    public void start() throws IOException {
        // Bind to all configured ports
        for (int port : config.getPorts()) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(config.getHost(), port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            Logger.info(TAG, "Listening on " + config.getHost() + ":" + port);
        }
        
        running = true;
        Logger.info(TAG, "Event loop started");
        
        // Main event loop
        while (running) {
            try {
                // Wait for events (with timeout for checking connection timeouts)
                int readyKeys = selector.select(TIMEOUT_CHECK_INTERVAL);
                
                // Check for timed out connections periodically
                checkTimeouts();
                
                if (readyKeys == 0) {
                    continue;
                }
                
                // Process ready keys
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    
                    try {
                        if (!key.isValid()) {
                            continue;
                        }
                        
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        Logger.error(TAG, "Error handling key", e);
                        closeConnection(key);
                    }
                }
            } catch (IOException e) {
                Logger.error(TAG, "Error in event loop", e);
            }
        }
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            
            // Create connection handler and attach to key
            ConnectionHandler handler = new ConnectionHandler(clientChannel, clientKey);
            clientKey.attach(handler);
            timeoutManager.register(handler);
            
            Logger.info(TAG, "Accepted connection from: " + handler.getRemoteAddress());
        }
    }
    
    private void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler != null) {
            handler.read();
        }
    }
    
    private void handleWrite(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler != null) {
            handler.write();
        }
    }
    
    private void closeConnection(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler != null) {
            timeoutManager.unregister(handler);
            handler.close();
        } else {
            try {
                key.channel().close();
            } catch (IOException e) {
                Logger.error(TAG, "Error closing channel", e);
            }
            key.cancel();
        }
    }
    
    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        if (now - lastTimeoutCheck >= TIMEOUT_CHECK_INTERVAL) {
            timeoutManager.checkTimeouts();
            lastTimeoutCheck = now;
        }
    }
    
    public void stop() {
        running = false;
        selector.wakeup();
        Logger.info(TAG, "Event loop stopping...");
    }
    
    public int getActiveConnections() {
        return timeoutManager.getActiveConnections();
    }
}