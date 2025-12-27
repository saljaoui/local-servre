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
            channel.configureBlocking(false);

            InetSocketAddress address = new InetSocketAddress(
                    config.getServers().get(0).getDefaultListen().getHost(),
                    config.getServers().get(0).getDefaultListen().getPort());

            channel.bind(address);
            channel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server running on http://"
                    + address.getHostString() + ":" + address.getPort());

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

                        int bytesRead = client.read(buffer);

                        // if does NOT guarantee full request
                        if (bytesRead == -1) {
                            client.close();
                            return;
                        }

                        buffer.flip(); // flip buffer to read mode

                        byte[] data = new byte[buffer.remaining()]; // create array for received data
                        buffer.get(data); // copy bytes from buffer
                        
                                          // print what client sent
                        System.out.println("----- REQUEST -----");
                        System.out.println(new String(data));
                        System.out.println("-------------------");

                        String body = "Hello omar & simo from soufian";

                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain; charset=UTF-8\r\n" +
                                "Content-Length: " + body.getBytes().length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n" +
                                body;

                        client.write(ByteBuffer.wrap(response.getBytes()));
                        client.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
