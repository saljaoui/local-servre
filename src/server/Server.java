package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

import config.model.WebServerConfig;

public class Server {

    private final WebServerConfig config;

    public Server(WebServerConfig config) {
        this.config = config; // save configuration (host + port)
    }

    public void start() {
        try {
            // Create the selector to handle multiple channels/events
            Selector selector = Selector.open();

            List<ServerSocketChannel> serverChannels = new ArrayList<>();
            for (WebServerConfig.ServerBlock serverBlock : config.getServers()) {
                for (WebServerConfig.ListenAddress addr : serverBlock.getListen()) {
                    ServerSocketChannel channel = ServerSocketChannel.open();
                    channel.configureBlocking(false);
                    channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()));
                    // attach server block so accept path knows which logical server this channel
                    // belongs to
                    channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);
                    serverChannels.add(channel);
                    System.out.println("Server running on http://"
                            + addr.getHost() + ":" + addr.getPort());
                }
            }

            // Call EventLoop to start processing client connections
            EventLoop.loop(selector);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
