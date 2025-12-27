package config.model;

import java.util.*;

public class WebServerConfig {
    
    private Timeouts timeouts;
    private List<ServerBlock> servers;

    // Getters and Setters
    public Timeouts getTimeouts() { return timeouts; }
    public void setTimeouts(Timeouts timeouts) { this.timeouts = timeouts; }
    
    public List<ServerBlock> getServers() { return servers; }
    public void setServers(List<ServerBlock> servers) { this.servers = servers; }

    // ========== NESTED CLASSES ==========
    
    public static class Timeouts {
        private int headerMillis;
        private int bodyMillis;
        private int keepAliveMillis;

        public int getHeaderMillis() { return headerMillis; }
        public void setHeaderMillis(int headerMillis) { this.headerMillis = headerMillis; }
        
        public int getBodyMillis() { return bodyMillis; }
        public void setBodyMillis(int bodyMillis) { this.bodyMillis = bodyMillis; }
        
        public int getKeepAliveMillis() { return keepAliveMillis; }
        public void setKeepAliveMillis(int keepAliveMillis) { this.keepAliveMillis = keepAliveMillis; }

        @Override
        public String toString() {
            return "Timeouts{header=" + headerMillis + "ms, body=" + bodyMillis + 
                   "ms, keepAlive=" + keepAliveMillis + "ms}";
        }
    }

    public static class ServerBlock {
        private String name;
        private List<ListenAddress> listen;
        private List<String> serverNames;
        private String root;
        private long clientMaxBodyBytes;
        private Map<String, String> errorPages;
        private List<Route> routes;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<ListenAddress> getListen() { return listen; }
        public void setListen(List<ListenAddress> listen) { this.listen = listen; }
        
        public List<String> getServerNames() { return serverNames; }
        public void setServerNames(List<String> serverNames) { this.serverNames = serverNames; }
        
        public String getRoot() { return root; }
        public void setRoot(String root) { this.root = root; }
        
        public long getClientMaxBodyBytes() { return clientMaxBodyBytes; }
        public void setClientMaxBodyBytes(long clientMaxBodyBytes) { 
            this.clientMaxBodyBytes = clientMaxBodyBytes; 
        }
        
        public Map<String, String> getErrorPages() { return errorPages; }
        public void setErrorPages(Map<String, String> errorPages) { 
            this.errorPages = errorPages; 
        }
        
        public List<Route> getRoutes() { return routes; }
        public void setRoutes(List<Route> routes) { this.routes = routes; }

        // Utility methods
        public ListenAddress getDefaultListen() {
            if (listen == null) return null;
            for (ListenAddress addr : listen) {
                if (addr.isDefault()) return addr;
            }
            return listen.isEmpty() ? null : listen.get(0);
        }

        public Route findRoute(String path) {
            if (routes == null) return null;
            
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
            if (errorPages == null) return null;
            return errorPages.get(String.valueOf(statusCode));
        }

        @Override
        public String toString() {
            return "ServerBlock{name='" + name + "', listen=" + listen + 
                   ", serverNames=" + serverNames + ", routes=" + 
                   (routes != null ? routes.size() : 0) + "}";
        }
    }

    public static class ListenAddress {
        private String host;
        private int port;
        private boolean defaultServer;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public boolean isDefault() { return defaultServer; }
        public void setDefault(boolean defaultServer) { this.defaultServer = defaultServer; }

        @Override
        public String toString() {
            return host + ":" + port + (defaultServer ? " (default)" : "");
        }
    }

    public static class Route {
        private String path;
        private List<String> methods;
        private String root;
        private String index;
        private boolean autoIndex;
        private Upload upload;
        private Cgi cgi;
        private Redirect redirect;

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        
        public String getRoot() { return root; }
        public void setRoot(String root) { this.root = root; }
        
        public String getIndex() { return index; }
        public void setIndex(String index) { this.index = index; }
        
        public boolean isAutoIndex() { return autoIndex; }
        public void setAutoIndex(boolean autoIndex) { this.autoIndex = autoIndex; }
        
        public Upload getUpload() { return upload; }
        public void setUpload(Upload upload) { this.upload = upload; }
        
        public Cgi getCgi() { return cgi; }
        public void setCgi(Cgi cgi) { this.cgi = cgi; }
        
        public Redirect getRedirect() { return redirect; }
        public void setRedirect(Redirect redirect) { this.redirect = redirect; }

        // Utility methods
        public boolean isMethodAllowed(String method) {
            if (methods == null || methods.isEmpty()) return true;
            return methods.stream().anyMatch(m -> m.equalsIgnoreCase(method));
        }

        public boolean isUploadEnabled() {
            return upload != null && upload.isEnabled();
        }

        public boolean isCgiEnabled() {
            return cgi != null && cgi.isEnabled();
        }

        public boolean isRedirect() {
            return redirect != null;
        }

        @Override
        public String toString() {
            return "Route{path='" + path + "', methods=" + methods + 
                   ", root='" + root + "'}";
        }
    }

    public static class Upload {
        private boolean enabled;
        private String dir;
        private String fileField;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        
        public String getFileField() { return fileField; }
        public void setFileField(String fileField) { this.fileField = fileField; }

        @Override
        public String toString() {
            return "Upload{enabled=" + enabled + ", dir='" + dir + 
                   "', fileField='" + fileField + "'}";
        }
    }

    public static class Cgi {
        private boolean enabled;
        private String binDir;
        private Map<String, String> byExtension;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getBinDir() { return binDir; }
        public void setBinDir(String binDir) { this.binDir = binDir; }
        
        public Map<String, String> getByExtension() { return byExtension; }
        public void setByExtension(Map<String, String> byExtension) { 
            this.byExtension = byExtension; 
        }

        public String getInterpreterForExtension(String ext) {
            if (byExtension == null) return null;
            return byExtension.get(ext.toLowerCase());
        }

        @Override
        public String toString() {
            return "Cgi{enabled=" + enabled + ", binDir='" + binDir + 
                   "', extensions=" + (byExtension != null ? byExtension.keySet() : "[]") + "}";
        }
    }

    public static class Redirect {
        private int status;
        private String to;

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        @Override
        public String toString() {
            return "Redirect{status=" + status + ", to='" + to + "'}";
        }
    }

    // ========== UTILITY METHODS ==========

    public ServerBlock findServerByName(String serverName) {
        if (servers == null) return null;
        
        for (ServerBlock server : servers) {
            if (server.getServerNames() != null && 
                server.getServerNames().contains(serverName)) {
                return server;
            }
        }
        
        // Return first server as default
        return servers.isEmpty() ? null : servers.get(0);
    }

    public ServerBlock findServerByPort(int port) {
        if (servers == null) return null;
        
        for (ServerBlock server : servers) {
            if (server.getListen() != null) {
                for (ListenAddress addr : server.getListen()) {
                    if (addr.getPort() == port) {
                        return server;
                    }
                }
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
        
        for (ServerBlock server : servers) {
            if (server.getName() == null || server.getName().isEmpty()) {
                throw new IllegalArgumentException("Server name is required");
            }
            
            if (server.getListen() == null || server.getListen().isEmpty()) {
                throw new IllegalArgumentException("Server '" + server.getName() + 
                                                   "' must have at least one listen address");
            }
            
            if (server.getRoutes() == null || server.getRoutes().isEmpty()) {
                throw new IllegalArgumentException("Server '" + server.getName() + 
                                                   "' must have at least one route");
            }
            
            // Check for required error pages
            if (server.getErrorPages() == null || server.getErrorPages().isEmpty()) {
                throw new IllegalArgumentException("Server '" + server.getName() + 
                                                   "' must have error pages defined");
            }
            
            String[] requiredErrors = {"400", "403", "404", "405", "413", "500"};
            for (String code : requiredErrors) {
                if (!server.getErrorPages().containsKey(code)) {
                    throw new IllegalArgumentException("Server '" + server.getName() + 
                                                       "' missing error page for: " + code);
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