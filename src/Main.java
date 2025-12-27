import config.WebConfigLoader;
import config.model.WebServerConfig;
import server.Server;

public class Main {
    public static void main(String[] args)  {

        WebServerConfig config = WebConfigLoader.load();

        Server server = new Server(config);
        server.start();

    }
}
