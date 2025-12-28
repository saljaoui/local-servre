package server;

import java.io.IOException;
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
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                } catch (IOException e) {
                    logger.error("Error handling connection", e);
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) {
        if (key.channel() instanceof ServerSocketChannel serverChannel) {
            try {
                var clientChannel = serverChannel.accept();
                if (clientChannel == null) {
                    return;
                }
                // Configure the client channel to be non-blocking
                clientChannel.configureBlocking(false);
                clientChannel.socket().setKeepAlive(true);
                clientChannel.socket().setTcpNoDelay(true);
                // Register the client channel with the selector for read operations
                // get server config attached to server socket key
                var srv = (config.model.WebServerConfig.ServerBlock) key.attachment();
                // register client and attach a ConnectionHandler to manage I/O
                SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
                ConnectionHandler handler = new ConnectionHandler(clientChannel, clientKey, srv);
                clientKey.attach(handler);
                // Create ConnectionHandler and attach to client's key
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleRead(SelectionKey key) throws IOException {
        ConnectionHandler handler = (ConnectionHandler) key.attachment();
        if (handler == null) {
            key.cancel();
            return;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        try {
            boolean readComplete = handler.read();
            if (readComplete) {
                handler.setState(ConnectionHandler.State.PROCESSING);

                try {
                    // Process the request
                    handler.processRequest();

                    // Change interest to write so selector will notify when writable
                    key.interestOps(SelectionKey.OP_WRITE);
                } catch (Exception e) {
                    logger.error("Error processing request: " + e.getMessage(), e);
                    handler.sendErrorResponse(500, "Internal Server Error");
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }

        } catch (IOException e) {
            logger.debug("Client disconnected: " + e.getMessage());
            try {
                channel.close();
            } catch (IOException ignore) {
            }
            key.cancel();
        }
    }

    private void handleWrite(SelectionKey key) {
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