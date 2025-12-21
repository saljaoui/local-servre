import config.ConfigLoader;
import config.ServerConfig;
import server.Server;
import utils.Logger;

public class Main {
    public static void main(String[] args) {
        String configPath = "config.json";

        // Allow custom config path as argument
        if (args.length > 0) {
            configPath = args[0];
        }

        try {
            Logger.info("Main", "Loading configuration from: " + configPath);

            // Step 1: Load configuration
            ServerConfig config = ConfigLoader.load(configPath);
            Logger.info("Main", "Configuration loaded successfully");

            // Print loaded routes
            Logger.info("Main", "Loaded " + config.getRoutes().size() + " routes:");
            for (ServerConfig.RouteConfig route : config.getRoutes()) {
                // System.out.println("Main.main() "+route.getCgiExtension() +" "+ +route.getRedirectCode());
                String target = route.isRedirect()
                        ? "redirect to " + route.getRedirectUrl()
                        : route.getRoot();
                Logger.info("Main", "  " + route.getPath() + " -> " + target);
            }

            // Step 2: Create and start server
            Server server = new Server(config);
            server.start();

        } catch (Exception e) {
            Logger.error("Main", "Failed to start server", e);
            System.exit(1);
        }
    }
}