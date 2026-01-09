package server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import config.model.WebServerConfig;
import util.SonicLogger;

public class EventLoop {

    private static final SonicLogger logger = SonicLogger.getLogger(EventLoop.class);
    private static final long HEADER_TIMEOUT_MS = 10_000;
    private static final long DEFAULT_BODY_IDLE_TIMEOUT_MS = 15 * 60 * 1000;
    private static final Map<SocketChannel, ConnActivity> connectionActivity = new HashMap<>();

    public static void loop(Selector selector, WebServerConfig config) throws IOException {
        logger.info("EventLoop started thread:" + Thread.currentThread().getName());

        long bodyIdleTimeoutMs = config.getTimeouts() > 0 ? config.getTimeouts() : DEFAULT_BODY_IDLE_TIMEOUT_MS;
        logger.info("Timeouts configured: header=" + HEADER_TIMEOUT_MS + "ms, bodyIdle=" + bodyIdleTimeoutMs + "ms");

        while (true) {
            checkTimeouts(selector, bodyIdleTimeoutMs);

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

    private static void checkTimeouts(Selector selector, long bodyIdleTimeoutMs) {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<SocketChannel, ConnActivity>> iter = connectionActivity.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<SocketChannel, ConnActivity> entry = iter.next();
            SocketChannel channel = entry.getKey();
            ConnActivity activity = entry.getValue();
            SelectionKey key = channel.keyFor(selector);

            if (key == null || !key.isValid()) {
                iter.remove();
                continue;
            }

            ConnectionHandler handler = (ConnectionHandler) key.attachment();
            if (handler == null) {
                iter.remove();
                continue;
            }

            long timeoutMs = handler.isReadingHeaders() ? HEADER_TIMEOUT_MS : bodyIdleTimeoutMs;
            long elapsed = currentTime - activity.lastActivityMs;

            if (elapsed > timeoutMs) {
                logger.info("Timeout for client: " + channel.socket().getRemoteSocketAddress() +
                        " (idle for " + elapsed + "ms)");

                handler.forceError(http.model.HttpStatus.REQUEST_TIMEOUT);
                key.interestOps(SelectionKey.OP_WRITE);

                iter.remove();
            }
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
        ConnectionHandler handler = new ConnectionHandler(clientChannel, portContext.getDefaultServer());
        clientKey.attach(handler); // Attach handler to the key

        // Track connection activity
        connectionActivity.put(clientChannel, new ConnActivity(System.currentTimeMillis()));

        // logger.info("Client connected: " + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            boolean requestComplete = handler.read(handler.getServer());
            if (handler.getLastReadBytes() > 0) {
                touchActivity(channel);
            }

            if (requestComplete) {
                handler.dispatchRequest();
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
            // handler.cleanupTempFile();

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

        SocketChannel channel = (SocketChannel) key.channel();

        try {
            boolean finished = handler.write();
            if (handler.getLastWriteBytes() > 0) {
                touchActivity(channel);
            }
            if (finished) {
                closeConnection(key);
            }
        } catch (IOException e) {
            try {
                closeConnection(key);
            } catch (Exception ignore) {
            }
        }
    }

    private static void touchActivity(SocketChannel channel) {
        ConnActivity activity = connectionActivity.get(channel);
        if (activity != null) {
            activity.lastActivityMs = System.currentTimeMillis();
        } else {
            connectionActivity.put(channel, new ConnActivity(System.currentTimeMillis()));
        }
    }

    private static final class ConnActivity {
        private long lastActivityMs;

        private ConnActivity(long lastActivityMs) {
            this.lastActivityMs = lastActivityMs;
        }
    }
}
