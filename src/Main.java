import config.WebConfigLoader;
import config.model.WebServerConfig;
import server.Server;
import util.SonicLogger;

public class Main {
    private static final SonicLogger logger = SonicLogger.getLogger(Main.class);
    
    public static void main(String[] args) {
        // SonicLogger.setMinLevel(SonicLogger.LogLevel.INFO);
        
        logger.info("Starting SonicServe HTTP Server");
        logger.info("Loading configuration from config/config.json");
        
        WebServerConfig config = WebConfigLoader.load();
        logger.success("Configuration loaded successfully");
        
        logger.info("Initializing " + config.getServers().size() + " server instance(s)");
        Server server = new Server(config);
        logger.success("All servers initialized successfully");
        
        logger.info("Starting all configured servers...");
        server.start();
    }
}