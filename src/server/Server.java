package server;

import config.model.WebServerConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {

    private final WebServerConfig config;

    public Server(WebServerConfig config) {
        this.config = config;
    }

    public void start() {
        try {

            Selector selector = Selector.open();

            List<ServerSocketChannel> serverChannels = new ArrayList<>();
            for (WebServerConfig.ServerBlock serverBlock : config.getServers()) {
                for (WebServerConfig.ListenAddress addr : serverBlock.getListen()) {
                    ServerSocketChannel channel = ServerSocketChannel.open();
                    channel.configureBlocking(false);
                    channel.bind(new InetSocketAddress(addr.getHost(), addr.getPort()));
                    // attach server block so accept path knows which logical server this channel belongs to
                    channel.register(selector, SelectionKey.OP_ACCEPT, serverBlock);
                    serverChannels.add(channel);
                                System.out.println("Server running on http://"
                    + addr.getHost() + ":" + addr.getPort());
                }
            }



            while (true) {
                selector.select(); // wait for events

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isAcceptable()) {
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel client = serverChannel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ, key.attachment());
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
