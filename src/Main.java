import config.WebConfigLoader;
import config.model.WebServerConfig;

public class Main {
    public static void main(String[] args) {
        WebServerConfig objectConfig = WebConfigLoader.load("./config/config.json");
        System.out.println(objectConfig);
    }
}
