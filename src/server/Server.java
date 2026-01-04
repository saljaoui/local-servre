package server;

import config.model.WebServerConfig;
import config.model.WebServerConfig.ListenAddress;
import config.model.WebServerConfig.ServerBlock;
import util.SonicLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final SonicLogger logger = SonicLogger.getLogger(Server.class);

    private final WebServerConfig config;
    private final Map<Integer, ServerSocketChannel> serverChannels = new HashMap<>();

    private Selector selector;

    public Server(WebServerConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            selector = Selector.open();
            registerShutdownHook();
            bindAllServers();

            logger.success("Server started with " + serverChannels.size() + " listener(s)");

            EventLoop.loop(selector, config);

        } catch (IOException e) {
            logger.error("Failed to start server", e);
            // throw e;
        } finally {
            cleanup();
        }
    }

    private void bindAllServers() throws IOException {
        try {
            for (ServerBlock serverBlock : config.getServers()) {
                bindSingleServer(serverBlock.getListen(), serverBlock);
            }
        } catch (IOException e) {
            closeAllChannels();
            throw e;
        }
    }

    private void bindSingleServer(ListenAddress addr, ServerBlock serverBlock) throws IOException {

        ServerSocketChannel channel = null;

        try {
            channel = ServerSocketChannel.open(); // ktft7 TCP server socket
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()));
            channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);
            serverChannels.put(addr.getPort(), channel);

            String url = "http://" + addr.getHost() + ":" + addr.getPort();
            String coloredUrl = "\u001B[38;5;51m" + url + "\u001B[38;5;51m";
            logger.success("Listening on " + coloredUrl);

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
            System.err.println();
            logger.info("Shutdown signal received");
            shutdown();
        }, "shutdown-hook"));
    }

    public void shutdown() {
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
}
