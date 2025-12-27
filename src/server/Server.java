package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

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

            // Open server socket channel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            // Bind to host and port from config
            InetSocketAddress address = new InetSocketAddress(
                    config.getServers().get(0).getDefaultListen().getHost(),
                    config.getServers().get(0).getDefaultListen().getPort()
            );
            serverChannel.bind(address);

            // Register server channel with selector to accept new clients
            serverChannel.register(selector, java.nio.channels.SelectionKey.OP_ACCEPT);

            // Print server info
            System.out.println("Server running on http://" + address.getHostString() + ":" + address.getPort());

            // Call EventLoop to start processing client connections
            EventLoop.loop(selector, serverChannel);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
