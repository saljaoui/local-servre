package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import config.model.WebServerConfig;

public class Server {

    private final WebServerConfig config;

    public Server(WebServerConfig config) {
        this.config = config;
    }

    public void start() {

        try {
            Selector selector = Selector.open();

            ServerSocketChannel channel = ServerSocketChannel.open();

            channel.bind(new InetSocketAddress(config.getServers().get(0).getDefaultListen().getPort()));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server running on http://localhost:" + "8080");
            while (true) {
                selector.select(); // wait for events
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel client = channel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        client.read(buffer);

                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Length: 5\r\n" +
                                "\r\n" +
                                "Hello omar & simo from soufian";

                        client.write(ByteBuffer.wrap(response.getBytes()));
                        client.close();
                    }
                }
            }
        } catch (IOException e) {

        }
    }
}
