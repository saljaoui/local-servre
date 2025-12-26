// src/config/ConfigValidator.java
package config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import config.model.AppConfig;
import config.model.Listen;
import config.model.RouteConfig;
import config.model.ServerConfig;

public final class ConfigValidator {
    private ConfigValidator() {}

    public static void validateOrExit(AppConfig cfg) {
        try {
            validate(cfg);
        } catch (Exception e) {
            System.err.println("ERROR: invalid config: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void validate(AppConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("Config is null");
        if (cfg.getServers() == null || cfg.getServers().isEmpty())
            throw new IllegalArgumentException("servers[] must not be empty");

        // duplicate listen check (host:port)
        Set<String> binds = new HashSet<>();

        for (ServerConfig s : cfg.getServers()) {
            if (s.getListen() == null || s.getListen().isEmpty())
                throw new IllegalArgumentException("server '" + s.getName() + "': listen[] empty");

            if (s.getRoot() == null || s.getRoot().isBlank())
                throw new IllegalArgumentException("server '" + s.getName() + "': root missing");

            for (Listen l : s.getListen()) {
                String key = l.getHost() + ":" + l.getPort();
                if (!binds.add(key)) throw new IllegalArgumentException("Duplicate listen: " + key);
                if (l.getPort() <= 0 || l.getPort() > 65535)
                    throw new IllegalArgumentException("Invalid port: " + l.getPort());
            }

            List<RouteConfig> routes = s.getRoutes();
            if (routes == null || routes.isEmpty())
                throw new IllegalArgumentException("server '" + s.getName() + "': routes[] empty");

            for (RouteConfig r : routes) {
                if (r.getPath() == null || !r.getPath().startsWith("/"))
                    throw new IllegalArgumentException("route path must start with '/': " + r.getPath());

                if (r.getMethods() == null || r.getMethods().isEmpty())
                    throw new IllegalArgumentException("route '" + r.getPath() + "': methods[] empty");

                if (r.getUpload() != null && r.getUpload().isEnabled()) {
                    if (r.getUpload().getDir() == null || r.getUpload().getDir().isBlank())
                        throw new IllegalArgumentException("route '" + r.getPath() + "': upload enabled but dir missing");
                }

                if (r.getCgi() != null && r.getCgi().isEnabled()) {
                    if (r.getCgi().getBinDir() == null || r.getCgi().getBinDir().isBlank())
                        throw new IllegalArgumentException("route '" + r.getPath() + "': cgi enabled but binDir missing");
                }

                if (r.getRedirect() != null) {
                    int st = r.getRedirect().getStatus();
                    if (st < 300 || st > 399)
                        throw new IllegalArgumentException("route '" + r.getPath() + "': redirect status must be 3xx");
                    String to = r.getRedirect().getTo();
                    if (to == null || !to.startsWith("/"))
                        throw new IllegalArgumentException("route '" + r.getPath() + "': redirect 'to' must start with '/'");
                }
            }
        }
    }
}
