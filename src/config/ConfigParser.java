package config;

import java.util.Map;

import config.model.AppConfig;

public final class ConfigParser {
    private static final String DEFAULT_PATH = "config/config.json";
    private ConfigParser() {}

    public static AppConfig parseConfig() {
        String strJson = ConfigFileChecker.readOrExit(DEFAULT_PATH);

        Object raw = new JsonParser(strJson).parseValue();
        if (!(raw instanceof Map)) {
            System.err.println("ERROR: config root must be a JSON object");
            System.exit(1);
        }

        AppConfig objectConfig = ConfigMapper.toAppConfig(raw);

        ConfigValidator.validateOrExit(objectConfig);

        return objectConfig;
    }
}
