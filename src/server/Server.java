package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

import config.model.WebServerConfig;
import util.SonicLogger;

public class Server {

    private static final SonicLogger logger = SonicLogger.getLogger(Server.class);
    private final WebServerConfig config;

    public Server(WebServerConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            logger.info("Opening selector for event-driven I/O");
            Selector selector = Selector.open();

            List<ServerSocketChannel> serverChannels = new ArrayList<>();
            
            logger.info("Binding " + config.getServers().size() + " server(s) to network interfaces");
            
            for (WebServerConfig.ServerBlock serverBlock : config.getServers()) {
                for (WebServerConfig.ListenAddress addr : serverBlock.getListen()) {
                    ServerSocketChannel channel = ServerSocketChannel.open();
                    channel.configureBlocking(false);
                    channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()));
                    channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);
                    serverChannels.add(channel);
                    
                    logger.success("Server listening on http://" + addr.getHost() + ":" + addr.getPort());
                }
            }
            
            EventLoop.loop(selector);

        } catch (IOException e) {
            logger.error("Failed to start server", e);
        }
    }
}