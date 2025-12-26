import config.ConfigParser;
import config.model.AppConfig;

public class Main {
    public static void main(String[] args) {
        AppConfig objectConfig = ConfigParser.parseConfig();
    }
}
