package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

import util.SonicLogger;

public class EventLoop {

    private static final SonicLogger logger = SonicLogger.getLogger(EventLoop.class);

    public static void loop(Selector selector) throws IOException {

        while (true) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel client = serverChannel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }
                    else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = client.read(buffer);

                        if (bytesRead == -1) {
                            client.close();
                            continue;
                        }

                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);

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
                } catch (IOException e) {
                    logger.error("Error handling connection", e);
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }
}