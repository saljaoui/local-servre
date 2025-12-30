package config.model;

import java.util.*;

import routing.model.Route;

public class WebServerConfig {

    private Timeouts timeouts;
    private List<ServerBlock> servers;

    // Getters and Setters
    public Timeouts getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
    }

    public List<ServerBlock> getServers() {
        return servers;
    }

    public void setServers(List<ServerBlock> servers) {
        this.servers = servers;
    }

    // ========== NESTED CLASSES ==========
    public static class Timeouts {

        private int headerMillis;
        private int bodyMillis;
        private int keepAliveMillis;

        public int getHeaderMillis() {
            return headerMillis;
        }

        public void setHeaderMillis(int headerMillis) {
            this.headerMillis = headerMillis;
        }

        public int getBodyMillis() {
            return bodyMillis;
        }

        public void setBodyMillis(int bodyMillis) {
            this.bodyMillis = bodyMillis;
        }

        public int getKeepAliveMillis() {
            return keepAliveMillis;
        }

        public void setKeepAliveMillis(int keepAliveMillis) {
            this.keepAliveMillis = keepAliveMillis;
        }

        @Override
        public String toString() {
            return "Timeouts{header=" + headerMillis + "ms, body=" + bodyMillis
                    + "ms, keepAlive=" + keepAliveMillis + "ms}";
        }
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

    public static class Upload {

        private boolean enabled;
        private String dir;
        private String fileField;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }

        public String getFileField() {
            return fileField;
        }

        public void setFileField(String fileField) {
            this.fileField = fileField;
        }

        @Override
        public String toString() {
            return "Upload{enabled=" + enabled + ", dir='" + dir
                    + "', fileField='" + fileField + "'}";
        }
    }

    public static class Cgi {

        private boolean enabled;
        private String binDir;
        private Map<String, String> byExtension;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBinDir() {
            return binDir;
        }

        public void setBinDir(String binDir) {
            this.binDir = binDir;
        }

        public Map<String, String> getByExtension() {
            return byExtension;
        }

        public void setByExtension(Map<String, String> byExtension) {
            this.byExtension = byExtension;
        }

        public String getInterpreterForExtension(String ext) {
            if (byExtension == null) {
                return null;
            }
            return byExtension.get(ext.toLowerCase());
        }

        @Override
        public String toString() {
            return "Cgi{enabled=" + enabled + ", binDir='" + binDir
                    + "', extensions=" + (byExtension != null ? byExtension.keySet() : "[]") + "}";
        }
    }

    public static class Redirect {

        private int status;
        private String to;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        @Override
        public String toString() {
            return "Redirect{status=" + status + ", to='" + to + "'}";
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

    public void validate() throws IllegalArgumentException {
        if (timeouts == null) {
            throw new IllegalArgumentException("Timeouts configuration is required");
        }

        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("At least one server block is required");
        }

        // Cross-server validation (unique host:port + one default per port)
        Set<String> listenKeys = new HashSet<>();
        Set<Integer> defaultPorts = new HashSet<>();

        for (ServerBlock server : servers) {
            if (server.getName() == null || server.getName().isEmpty()) {
                throw new IllegalArgumentException("Server name is required");
            }

            if (server.getListen() == null) {
                throw new IllegalArgumentException("Server '" + server.getName()
                        + "' must have a listen address");
            }

            // Validate listen entry
            ListenAddress addr = server.getListen();
            String host = (addr.getHost() == null || addr.getHost().trim().isEmpty())
                    ? "0.0.0.0"
                    : addr.getHost().trim();

            int port = addr.getPort();
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid port: " + port
                        + " in server '" + server.getName() + "'");
            }

            String key = host + ":" + port;
            if (!listenKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate listen address detected: " + key);
            }

            if (addr.isDefault() && !defaultPorts.add(port)) {
                throw new IllegalArgumentException("More than one default server on port " + port);
            }

            if (server.getRoutes() == null || server.getRoutes().isEmpty()) {
                throw new IllegalArgumentException("Server '" + server.getName()
                        + "' must have at least one route");
            }

            // Check for required error pages
            if (server.getErrorPages() == null || server.getErrorPages().isEmpty()) {
                throw new IllegalArgumentException("Server '" + server.getName()
                        + "' must have error pages defined");
            }

            String[] requiredErrors = {"400", "403", "404", "405", "413", "500"};
            for (String code : requiredErrors) {
                if (!server.getErrorPages().containsKey(code)) {
                    throw new IllegalArgumentException("Server '" + server.getName()
                            + "' missing error page for: " + code);
                }
            }
        }
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
}
