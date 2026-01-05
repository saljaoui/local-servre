package config.model;

import java.util.*;

import routing.model.Route;
import util.SonicLogger;

public class WebServerConfig {

    private long timeouts;
    private List<ServerBlock> servers;
    private static final SonicLogger logger = SonicLogger.getLogger(WebServerConfig.class);

    // Getters and Setters
    public long getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(long timeouts) {
        this.timeouts = timeouts;
    }

    public List<ServerBlock> getServers() {
        return servers;
    }

    public void setServers(List<ServerBlock> servers) {
        this.servers = servers;
    }

    public static class ServerBlock {

        private String name;
        private ListenAddress listen;
        private List<String> serverNames;
        private String root;
        private long clientMaxBodyBytes;
        private Map<String, String> errorPages;
        private List<Route> routes;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ListenAddress getListen() {
            return listen;
        }

        public void setListen(ListenAddress listen) {
            this.listen = listen;
        }

        public List<String> getServerNames() {
            return serverNames;
        }

        public void setServerNames(List<String> serverNames) {
            this.serverNames = serverNames;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public long getClientMaxBodyBytes() {
            return clientMaxBodyBytes;
        }

        public void setClientMaxBodyBytes(long clientMaxBodyBytes) {
            this.clientMaxBodyBytes = clientMaxBodyBytes;
        }

        public Map<String, String> getErrorPages() {
            return errorPages;
        }

        public void setErrorPages(Map<String, String> errorPages) {
            this.errorPages = errorPages;
        }

        public List<Route> getRoutes() {
            return routes;
        }

        public void setRoutes(List<Route> routes) {
            this.routes = routes;
        }

        public Route findRoute(String path) {
            if (routes == null) {
                return null;
            }

            // Exact match first
            for (Route route : routes) {
                if (route.getPath().equals(path)) {
                    return route;
                }
            }

            // Prefix match (longest match wins)
            Route bestMatch = null;
            int longestMatch = 0;

            for (Route route : routes) {
                String routePath = route.getPath();
                if (path.startsWith(routePath) && routePath.length() > longestMatch) {
                    bestMatch = route;
                    longestMatch = routePath.length();
                }
            }

            return bestMatch;
        }

        public String getErrorPage(int statusCode) {
            if (errorPages == null) {
                return null;
            }
            return errorPages.get(String.valueOf(statusCode));
        }

        @Override
        public String toString() {
            return "ServerBlock{name='" + name + "', listen=" + listen
                    + ", serverNames=" + serverNames + ", routes="
                    + (routes != null ? routes.size() : 0) + "}";
        }
    }

    public static class ListenAddress {

        private String host;
        private int port;
        private boolean defaultServer;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isDefault() {
            return defaultServer;
        }

        public void setDefault(boolean defaultServer) {
            this.defaultServer = defaultServer;
        }

        @Override
        public String toString() {
            return host + ":" + port + (defaultServer ? " (default)" : "");
        }
    }

   
    // ========== UTILITY METHODS ==========
    public ServerBlock findServerByName(String serverName) {
        if (servers == null) {
            return null;
        }

        for (ServerBlock server : servers) {
            if (server.getServerNames() != null
                    && server.getServerNames().contains(serverName)) {
                return server;
            }
        }

        // Return first server as default
        return servers.isEmpty() ? null : servers.get(0);
    }

    public ServerBlock findServerByPort(int port) {
        if (servers == null) {
            return null;
        }

        for (ServerBlock server : servers) {
            if (server.getListen() != null && server.getListen().getPort() == port) {
                return server;
            }
        }

        return null;
    }

    public List<String> validateAndPrune() {
        List<String> errors = new ArrayList<>();

        if (timeouts <= 0) {
            errors.add("Timeouts must be positive");
        }

        if (servers == null || servers.isEmpty()) {
            errors.add("At least one server block is required");
            return errors;
        }

        Set<String> serverNames = new HashSet<>();
        Map<Integer, PortInfo> portInfo = new HashMap<>();
        List<ServerBlock> validServers = new ArrayList<>();
        
        // Use a Set for O(1) lookups instead of List
        Set<String> validMethods = new HashSet<>(Arrays.asList("GET", "POST", "DELETE", "OPTIONS"));

        for (ServerBlock server : servers) {
            try {
                // Server name validation
                if (server.getName() == null || server.getName().isEmpty()) {
                    throw new IllegalArgumentException("Server name is required");
                }
                if (!serverNames.add(server.getName())) {
                    throw new IllegalArgumentException("Duplicate server name config: " + server.getName());
                }

                if (server.getListen() == null) {
                    throw new IllegalArgumentException("Server '" + server.getName() + "' must have a listen address");
                }

                ListenAddress addr = server.getListen();
                int port = addr.getPort();

                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("Invalid port: " + port + " in server '" + server.getName() + "'");
                }

                if (portInfo.containsKey(port)) {
                    logger.warn("Port " + port + " is shared by multiple server blocks (Server: " + server.getName() + ").");
                }

                portInfo.putIfAbsent(port, new PortInfo());
                PortInfo info = portInfo.get(port);

                if (addr.isDefault()) {
                    if (info.hasDefault) {
                        throw new IllegalArgumentException("More than one default server on port " + port);
                    }
                    info.hasDefault = true;
                }

                if (server.getServerNames() == null || server.getServerNames().isEmpty()) {
                    throw new IllegalArgumentException("Server '" + server.getName() + "' must have at least one server name");
                }

                for (String sName : server.getServerNames()) {
                    if (!info.names.add(sName)) {
                        throw new IllegalArgumentException("Duplicate serverName '" + sName + "' on port " + port);
                    }
                }

                if (server.getClientMaxBodyBytes() <= 0) {
                    throw new IllegalArgumentException("Server '" + server.getName() + "' must have positive clientMaxBodyBytes");
                }

                if (server.getRoutes() == null || server.getRoutes().isEmpty()) {
                    throw new IllegalArgumentException("Server '" + server.getName() + "' must have at least one route");
                }

                for (Route route : server.getRoutes()) {
                    if (route.getPath() == null || route.getPath().isEmpty()) {
                        throw new IllegalArgumentException("Route path is required in server '" + server.getName() + "'");
                    }

                    if (route.getMethods() == null || route.getMethods().isEmpty()) {
                        throw new IllegalArgumentException("Route '" + route.getPath() + "' must have at least one HTTP method");
                    }

                    for (String method : route.getMethods()) {
                        if (!validMethods.contains(method.toUpperCase())) {
                            throw new IllegalArgumentException("Invalid HTTP method '" + method + "' in route '" + route.getPath() + "'");
                        }
                    }
                }

                if (server.getErrorPages() == null || server.getErrorPages().isEmpty()) {
                    throw new IllegalArgumentException("Server '" + server.getName() + "' must have error pages defined");
                }

                String[] requiredErrors = {"400", "403", "404", "405", "413", "500"};
                for (String code : requiredErrors) {
                    if (!server.getErrorPages().containsKey(code)) {
                        throw new IllegalArgumentException("Server '" + server.getName() + "' missing error page for: " + code);
                    }
                }

                validServers.add(server);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        this.servers = validServers;
        if (validServers.isEmpty() && errors.isEmpty()) {
            errors.add("No valid servers after validation");
        }
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WebServerConfig{\n");
        sb.append("  timeouts=").append(timeouts).append("\n");
        sb.append("  servers=[\n");
        if (servers != null) {
            for (ServerBlock server : servers) {
                sb.append("    ").append(server).append("\n");
            }
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static class PortInfo {
        boolean hasDefault = false;
        Set<String> names = new HashSet<>();
    }
}
