package config;

import java.io.*;
import java.nio.file.*;

public class ConfigLoader {
    public static ServerConfig load(String filePath) {

        try {
            String content = Files.readString(Path.of(filePath));
            System.out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ServerConfig();
    }
}