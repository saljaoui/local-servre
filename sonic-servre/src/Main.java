import config.ConfigLoader;
import config.ServerConfig;

public class Main {
    public static void main(String[] args) {
        // System.out.println("mian start soso");
        ServerConfig config = ConfigLoader.load("config/config.json");
        // ServerConfig config = ConfigLoader.load("config/config.json");
        // Server server = new Server(config);
        // server.start();
    }
}