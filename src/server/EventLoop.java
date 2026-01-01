package server;

import config.model.WebServerConfig.ServerBlock;
import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import util.SonicLogger;

public class EventLoop {

    private static final SonicLogger logger = SonicLogger.getLogger(EventLoop.class);

    public static void loop(Selector selector) throws IOException {
        logger.info("EventLoop started thread:" + Thread.currentThread().getName());

        while (true) {
            // Wait for events to happen
            selector.select();

            // Get all events that happened
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove(); // Must remove after getting it

                try {
                    // Handle different types of events
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    }
                    if (!key.isValid()) continue;
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (!key.isValid()) continue;
                    if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        ServerBlock server = (ServerBlock) key.attachment();

        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null)
            return;

        // Configure client channel
        clientChannel.configureBlocking(false);
        clientChannel.socket().setTcpNoDelay(true); // Send data immediately

        // Register for READ events and attach a Handler
        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
        ConnectionHandler handler = new ConnectionHandler(clientChannel, server);
        clientKey.attach(handler); // Attach handler to the key

        System.out.println("Client connected: " + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }
        
        // Read data from client
        boolean readComplete = handler.read();

        if (readComplete) {
            // Process the request
            handler.dispatchRequest();

            // Switch to WRITE mode - we're ready to send response
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private static void handleWrite(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }
        try {
            boolean finished = handler.write();
            if (finished) {
                handler.close();
            }
        } catch (IOException e) {
            try {
                handler.close();
            } catch (Exception ignore) {
            }
        }
    }
}