package server;

import config.model.WebServerConfig;
import util.SonicLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {

    private static final SonicLogger logger = SonicLogger.getLogger(Server.class);
    private static final int SERVER_BACKLOG = 1024;

    private final WebServerConfig config;
    private final Map<Integer, ServerSocketChannel> serverChannels = new HashMap<>();
    
    private volatile boolean running = false;
    private Selector selector;

    public Server(WebServerConfig config) {
        if (config == null || config.getServers() == null || config.getServers().isEmpty()) {
            throw new IllegalArgumentException("Invalid server configuration");
        }
        this.config = config;
    }

    public void start() {
        if (running) {
            logger.warn("Server is already running");
            return;
        }
        
        running = true;

        try {
            selector = Selector.open();
            registerShutdownHook();
            bindAllServers();
            
            logger.success("Server started with " + serverChannels.size() + " listener(s)");
            
            EventLoop.loop(selector);
            
        } catch (IOException e) {
            logger.error("Failed to start server", e);
            // throw e;
        } finally {
            cleanup();
        }
    }

    private void bindAllServers() throws IOException {
        logger.info("Binding " + config.getServers().size() + " server(s)");
        
        Set<String> boundAddresses = new HashSet<>();
        
        try {
            for (WebServerConfig.ServerBlock serverBlock : config.getServers()) {
                bindServerBlock(serverBlock, boundAddresses);
            }
        } catch (IOException e) {
            closeAllChannels();
            throw e;
        }
    }

    private void bindServerBlock(WebServerConfig.ServerBlock serverBlock, Set<String> boundAddresses) 
            throws IOException {
        
        for (WebServerConfig.ListenAddress addr : serverBlock.getListen()) {
            String addressKey = addr.getHost() + ":" + addr.getPort();
            
            if (boundAddresses.contains(addressKey)) {
                logger.warn("Skipping duplicate address: " + addressKey);
                continue;
            }
            
            bindSingleServer(addr, serverBlock);
            boundAddresses.add(addressKey);
        }
    }

    private void bindSingleServer(WebServerConfig.ListenAddress addr, 
                                   WebServerConfig.ServerBlock serverBlock) throws IOException {
        
        ServerSocketChannel channel = null;
        
        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
            
            channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()), SERVER_BACKLOG);
            channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);
            
            serverChannels.put(addr.getPort(), channel);
            
            logger.success("Listening on http://" + addr.getHost() + ":" + addr.getPort());
            
        } catch (IOException e) {
            logger.error("Failed to bind " + addr.getHost() + ":" + addr.getPort(), e);
            if (channel != null) {
                safeClose(channel);
            }
            throw new IOException("Cannot bind to " + addr.getHost() + ":" + addr.getPort(), e);
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            shutdown();
        }, "shutdown-hook"));
    }

    public void shutdown() {
        if (!running) {
            return;
        }
        
        running = false;
        logger.info("Shutting down server...");
        
        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void cleanup() {
        logger.info("Cleaning up resources...");
        
        closeAllChannels();
        closeSelector();
        
        logger.success("Server stopped");
    }

    private void closeAllChannels() {
        for (Map.Entry<Integer, ServerSocketChannel> entry : serverChannels.entrySet()) {
            try {
                ServerSocketChannel channel = entry.getValue();
                if (channel.isOpen()) {
                    logger.info("Closing port " + entry.getKey());
                    channel.close();
                }
            } catch (IOException e) {
                logger.error("Error closing port " + entry.getKey(), e);
            }
        }
        serverChannels.clear();
    }

    private void closeSelector() {
        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                logger.error("Error closing selector", e);
            }
        }
    }

    private void safeClose(ServerSocketChannel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            logger.error("Error closing channel", e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getActiveListenerCount() {
        return serverChannels.size();
    }
}