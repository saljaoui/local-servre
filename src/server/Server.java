package server;

import config.model.WebServerConfig;
import util.SonicLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final SonicLogger logger = SonicLogger.getLogger(Server.class);

    private final WebServerConfig config;
    private final Map<Integer, ServerSocketChannel> serverChannels = new HashMap<>();

    private volatile boolean running = true;
    private Selector selector;

    public Server(WebServerConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            selector = Selector.open();

            // Ctrl+C -> shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            bindAllServers();

            // If you keep EventLoop.loop(selector), it should stop when selector is closed.
            EventLoop.loop(selector);

        } catch (IOException e) {
            logger.error("Failed to start server", e);
            shutdown();
        }
    }

    private void bindAllServers() throws IOException {
        logger.info("Binding " + config.getServers().size() + " server(s) to network interfaces");

        for (WebServerConfig.ServerBlock serverBlock : config.getServers()) {
            for (WebServerConfig.ListenAddress addr : serverBlock.getListen()) {

                // optional: prevent duplicate port in same process
                if (serverChannels.containsKey(addr.getPort())) {
                    throw new IOException("Port already bound in this process: " + addr.getPort());
                }

                ServerSocketChannel channel = ServerSocketChannel.open();
                try {
                    channel.configureBlocking(false);
                    channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()));
                    channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);

                    serverChannels.put(addr.getPort(), channel);
                    logger.success("Server listening on http://" + addr.getHost() + ":" + addr.getPort());

                } catch (IOException ex) {
                    try { channel.close(); } catch (IOException ignore) {}
                    throw ex;
                }
            }
        }
    }

    public void shutdown() {
        if (!running) return;
        running = false;

        logger.info("Shutting down server...");

        // Wake up select() so loop can notice shutdown / selector close
        if (selector != null) selector.wakeup();

        for (ServerSocketChannel ch : serverChannels.values()) {
            try { ch.close(); } catch (IOException ignore) {}
        }
        serverChannels.clear();

        if (selector != null) {
            try { selector.close(); } catch (IOException ignore) {}
        }

        logger.success("Server stopped.");
    }
}
