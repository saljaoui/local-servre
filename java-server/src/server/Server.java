package server;

import java.io.IOException;
import config.ServerConfig;
import utils.Logger;

public class Server {
    private static final String TAG = "Server";
    
    private final ServerConfig config;
    private EventLoop eventLoop;
    
    public Server(ServerConfig config) {
        this.config = config;
    }
    
    public void start() {
        Logger.info(TAG, "Starting LocalServer...");
        Logger.info(TAG, "Host: " + config.getHost());
        Logger.info(TAG, "Ports: " + config.getPorts());
        Logger.info(TAG, "Timeout: " + config.getTimeout() + "s");
        Logger.info(TAG, "Max body size: " + config.getClientMaxBodySize() + " bytes");
        
        try {
            // Initialize event loop
            eventLoop = new EventLoop(config);
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.info(TAG, "Shutdown signal received");
                stop();
            }));
            
            // Start the event loop (this blocks)
            eventLoop.start();
            
        } catch (IOException e) {
            Logger.error(TAG, "Failed to start server", e);
            System.exit(1);
        }
    }
    
    public void stop() {
        Logger.info(TAG, "Stopping server...");
        if (eventLoop != null) {
            eventLoop.stop();
        }
        Logger.info(TAG, "Server stopped");
    }
    
    public ServerConfig getConfig() {
        return config;
    }
}