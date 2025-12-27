package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class EventLoop {

    // The main selector loop
    public static void loop(Selector selector, ServerSocketChannel serverChannel) throws IOException {

        while (true) {
            selector.select(); // wait for events (blocking)

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove(); // remove key to prevent re-processing

                // 1️⃣ Accept new client connections
                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept(); // accept the client
                    client.configureBlocking(false); // non-blocking
                    client.register(selector, SelectionKey.OP_READ); // watch client for READ events
                }

                // 2️⃣ Handle client data
                else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();

                    ByteBuffer buffer = ByteBuffer.allocate(1024); // allocate buffer
                    int bytesRead = client.read(buffer);

                    if (bytesRead == -1) { // client closed connection
                        client.close();
                        continue;
                    }

                    buffer.flip(); // switch buffer to read mode
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    // print client request
                    System.out.println("----- REQUEST -----");
                    System.out.println(new String(data));
                    System.out.println("-------------------");

                    // simple HTTP response
                    String body = "Hello omar & simo from soufian";
                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=UTF-8\r\n" +
                            "Content-Length: " + body.getBytes().length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            body;

                    client.write(ByteBuffer.wrap(response.getBytes())); // send response
                    client.close(); // close connection
                }
            }
        }
    }
}
