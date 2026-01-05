package server;

import config.model.WebServerConfig;
import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import util.SonicLogger;

public class EventLoop {

    private static final SonicLogger logger = SonicLogger.getLogger(EventLoop.class);
    private static final Map<SocketChannel, Long> connectionActivity = new HashMap<>();

    public static void loop(Selector selector, WebServerConfig config) throws IOException {
        logger.info("EventLoop started thread:" + Thread.currentThread().getName());

        long timeoutMillis = config.getTimeouts();
        logger.info("Timeout configured: " + timeoutMillis + "ms");

        while (true) {
            checkTimeouts(timeoutMillis);

            // Wait for events (1 second timeout)
            selector.select(1000);

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
                    if (!key.isValid())
                        continue;
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (!key.isValid())
                        continue;
                    if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    logger.error("Error handling event: " + e.getMessage(), e);
                    closeConnection(key);
                }
            }
        }
    }

    static void removeTracking(SocketChannel channel) {
        connectionActivity.remove(channel);
    }

    private static void checkTimeouts(long timeoutMillis) {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<SocketChannel, Long>> iter = connectionActivity.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<SocketChannel, Long> entry = iter.next();
            long elapsed = currentTime - entry.getValue();

            if (elapsed > timeoutMillis) {
                SocketChannel channel = entry.getKey();
                logger.info("Timeout for client: " + channel.socket().getRemoteSocketAddress() +
                        " (idle for " + elapsed + "ms)");

                try {
                    sendTimeoutResponse(channel);
                    channel.close();
                } catch (IOException e) {
                    logger.error("Error closing timed out connection: " + e.getMessage());
                }

                iter.remove();
            }
        }
    }

    private static void sendTimeoutResponse(SocketChannel channel) {
        try {
            String response = "HTTP/1.1 408 Request Timeout\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: 50\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "<html><body><h1>408 Request Timeout</h1></body></html>";

            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(response.getBytes());
            channel.write(buffer);
        } catch (IOException e) {
            // Ignore errors when sending timeout response
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        Server.PortContext portContext = (Server.PortContext) key.attachment();

        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null)
            return;

        // Configure client channel
        clientChannel.configureBlocking(false);
        clientChannel.socket().setTcpNoDelay(true); // Send data immediately

        // Register for READ events and attach a Handler
        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
        ConnectionHandler handler = new ConnectionHandler(clientChannel, portContext);
        clientKey.attach(handler); // Attach handler to the key

        // Track connection activity
        connectionActivity.put(clientChannel, System.currentTimeMillis());

        // logger.info("Client connected: " + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();

        // Update activity time
        SocketChannel channel = (SocketChannel) key.channel();
        connectionActivity.put(channel, System.currentTimeMillis());

        try {
            // Read data from the channel
            boolean requestComplete = handler.read();

            if (handler.hasPendingResponse()) {
                key.interestOps(SelectionKey.OP_WRITE);
            } else if (requestComplete) {
                // Process the request
                handler.dispatchRequest();

                // Change interest to write so selector will notify when writable
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            // Client disconnected or error reading
            logger.debug("Client disconnected: " + e.getMessage());
            closeConnection(key);
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage(), e);
            closeConnection(key);
        }
    }

    private static void closeConnection(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler != null) {
            // Clean up any temporary files
            handler.cleanupTempFile();

            try {
                handler.close();
            } catch (IOException e) {
                logger.error("Error closing connection", e);
            }
        }

        try {
            connectionActivity.remove((SocketChannel) key.channel());
            key.channel().close();
        } catch (IOException e) {
            logger.error("Error closing channel", e);
        }
        key.cancel();
    }

    private static void handleWrite(SelectionKey key) {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }

        // Update activity time
        SocketChannel channel = (SocketChannel) key.channel();
        connectionActivity.put(channel, System.currentTimeMillis());

        try {
            boolean finished = handler.write();
            if (finished) {
                handler.close();
                connectionActivity.remove(channel);
            }
        } catch (IOException e) {
            try {
                handler.close();
                connectionActivity.remove(channel);
            } catch (Exception ignore) {
            }
        }
    }

}
