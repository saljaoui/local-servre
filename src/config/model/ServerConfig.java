package config.model;

import java.util.*;

public final class ServerConfig {
    private final String name;
    private final List<Listen> listen;
    private final List<String> serverNames;
    private final String root;
    private final long clientMaxBodyBytes;
    private final Map<Integer, String> errorPages;
    private final List<RouteConfig> routes;

    public ServerConfig(String name, List<Listen> listen, List<String> serverNames,
                        String root, long clientMaxBodyBytes,
                        Map<Integer, String> errorPages, List<RouteConfig> routes) {
        this.name = name;
        this.listen = listen;
        this.serverNames = serverNames;
        this.root = root;
        this.clientMaxBodyBytes = clientMaxBodyBytes;
        this.errorPages = errorPages;
        this.routes = routes;
    }

    public String getName() { return name; }
    public List<Listen> getListen() { return listen; }
    public List<String> getServerNames() { return serverNames; }
    public String getRoot() { return root; }
    public long getClientMaxBodyBytes() { return clientMaxBodyBytes; }
    public Map<Integer, String> getErrorPages() { return errorPages; }
    public List<RouteConfig> getRoutes() { return routes; }

    public int getDefaultPort() {
        for (Listen l : listen) if (l.isDefault()) return l.getPort();
        return listen.isEmpty() ? -1 : listen.get(0).getPort();
    }
}
