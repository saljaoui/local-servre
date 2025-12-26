// src/config/ConfigFileChecker.java
package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class ConfigFileChecker {
    private ConfigFileChecker() {}

    public static String readOrExit(String configPath) {
        Path p = Path.of(configPath);

        try {
            if (!Files.exists(p)) throw new NoSuchFileException(configPath);
            if (!Files.isRegularFile(p)) {
                System.err.println("ERROR: config path is not a file: " + configPath);
                System.exit(1);
            }
            if (!Files.isReadable(p)) throw new AccessDeniedException(configPath);

            return Files.readString(p, StandardCharsets.UTF_8);

        } catch (NoSuchFileException e) {
            System.err.println("ERROR: config file not found: " + configPath);
            System.err.println("Fix: run from project root and ensure it exists.");
            System.exit(1);
        } catch (AccessDeniedException e) {
            System.err.println("ERROR: no permission to read config file: " + configPath);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: failed to read config file: " + configPath);
            System.err.println("Reason: " + e.getMessage());
            System.exit(1);
        }

        return null; // unreachable
    }
}
