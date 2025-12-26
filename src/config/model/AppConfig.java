package config.model;

import java.util.*;

public final class AppConfig {
    private final Timeouts timeouts;
    private final List<ServerConfig> servers;

    public AppConfig(Timeouts timeouts, List<ServerConfig> servers) {
        this.timeouts = timeouts;
        this.servers = servers;
    }

    public Timeouts getTimeouts() { return timeouts; }
    public List<ServerConfig> getServers() { return servers; }

    // Convenience: default server + port
    public ServerConfig getDefaultServer() {
        for (ServerConfig s : servers) {
            for (Listen l : s.getListen()) if (l.isDefault()) return s;
        }
        return servers.isEmpty() ? null : servers.get(0);
    }

    public int getPort() {
        ServerConfig s = getDefaultServer();
        if (s == null) throw new IllegalStateException("No servers configured");
        return s.getDefaultPort();
    }

    @Override
    public String toString() {
        return "AppConfig{timeouts=" + timeouts + ", servers=" + servers + "}";
    }
}
