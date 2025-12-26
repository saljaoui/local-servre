import java.rmi.server.ServerCloneException;

import config.WebConfigLoader;
import config.model.WebServerConfig;
import server.Server;

public class Main {
    public static void main(String[] args) {

        WebServerConfig config = WebConfigLoader.load();
        config.validate();

        Server server = new Server(config);
        server.start();

    }
}
