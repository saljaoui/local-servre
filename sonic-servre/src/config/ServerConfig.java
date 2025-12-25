package config;

import java.util.*;

public class ServerConfig {
    
    // Server settings
    private ServerSettings server;
    private ContentSettings content;
    private Map<String, String> errorPages;
    private CgiSettings cgi;
    private SessionSettings sessions;
    private List<Route> routes;

    // Getters
    public ServerSettings getServer() { return server; }
    public ContentSettings getContent() { return content; }
    public Map<String, String> getErrorPages() { return errorPages; }
    public CgiSettings getCgi() { return cgi; }
    public SessionSettings getSessions() { return sessions; }
    public List<Route> getRoutes() { return routes; }

    // Setters
    public void setServer(ServerSettings server) { this.server = server; }
    public void setContent(ContentSettings content) { this.content = content; }
    public void setErrorPages(Map<String, String> errorPages) { this.errorPages = errorPages; }
    public void setCgi(CgiSettings cgi) { this.cgi = cgi; }
    public void setSessions(SessionSettings sessions) { this.sessions = sessions; }
    public void setRoutes(List<Route> routes) { this.routes = routes; }

    // Nested classes for structure
    
    public static class ServerSettings {
        private String host;
        private List<Integer> ports;
        private int defaultPort;
        private int timeout = 60;

        public String getHost() { return host; }
        public List<Integer> getPorts() { return ports; }
        public int getDefaultPort() { return defaultPort; }
        public int getTimeout() { return timeout; }

        public void setHost(String host) { this.host = host; }
        public void setPorts(List<Integer> ports) { this.ports = ports; }
        public void setDefaultPort(int defaultPort) { this.defaultPort = defaultPort; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    public static class ContentSettings {
        private long maxClientBodySize;
        private String defaultIndexFile;
        private boolean allowDirectoryListing;

        public long getMaxClientBodySize() { return maxClientBodySize; }
        public String getDefaultIndexFile() { return defaultIndexFile; }
        public boolean isAllowDirectoryListing() { return allowDirectoryListing; }

        public void setMaxClientBodySize(long maxClientBodySize) { 
            this.maxClientBodySize = maxClientBodySize; 
        }
        public void setDefaultIndexFile(String defaultIndexFile) { 
            this.defaultIndexFile = defaultIndexFile; 
        }
        public void setAllowDirectoryListing(boolean allowDirectoryListing) { 
            this.allowDirectoryListing = allowDirectoryListing; 
        }
    }

    public static class CgiSettings {
        private boolean enabled;
        private Map<String, String> extensions;

        public boolean isEnabled() { return enabled; }
        public Map<String, String> getExtensions() { return extensions; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setExtensions(Map<String, String> extensions) { 
            this.extensions = extensions; 
        }

        public String getInterpreterForExtension(String ext) {
            if (extensions == null) return null;
            return extensions.get(ext.toLowerCase().replace(".", ""));
        }
    }

    public static class SessionSettings {
        private boolean enabled;
        private String cookieName;

        public boolean isEnabled() { return enabled; }
        public String getCookieName() { return cookieName; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    }

    public static class Route {
        private String path;
        private String root;
        private List<String> methods;
        private String redirect;
        private Integer statusCode;
        private boolean cgiEnabled;
        private Boolean allowDirectoryListing;

        public String getPath() { return path; }
        public String getRoot() { return root; }
        public List<String> getMethods() { return methods; }
        public String getRedirect() { return redirect; }
        public Integer getStatusCode() { return statusCode; }
        public boolean isCgiEnabled() { return cgiEnabled; }
        public Boolean getAllowDirectoryListing() { return allowDirectoryListing; }

        public void setPath(String path) { this.path = path; }
        public void setRoot(String root) { this.root = root; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
        public void setCgiEnabled(boolean cgiEnabled) { this.cgiEnabled = cgiEnabled; }
        public void setAllowDirectoryListing(Boolean allowDirectoryListing) { 
            this.allowDirectoryListing = allowDirectoryListing; 
        }

        public boolean isMethodAllowed(String method) {
            if (methods == null || methods.isEmpty()) return true;
            return methods.stream()
                .anyMatch(m -> m.equalsIgnoreCase(method));
        }

        public boolean isRedirect() {
            return redirect != null && !redirect.isEmpty();
        }
    }

    // Utility methods
    
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

    public boolean isValidPort(int port) {
        return server != null && 
               server.getPorts() != null && 
               server.getPorts().contains(port);
    }

    public void validate() throws IllegalArgumentException {
        if (server == null || server.getHost() == null) {
            throw new IllegalArgumentException("Server host is required");
        }
        
        if (server.getPorts() == null || server.getPorts().isEmpty()) {
            throw new IllegalArgumentException("At least one port is required");
        }
        
        if (content == null) {
            throw new IllegalArgumentException("Content settings are required");
        }
        
        if (errorPages == null || errorPages.isEmpty()) {
            throw new IllegalArgumentException("Error pages are required");
        }
        
        // Check for required error pages
        String[] requiredErrors = {"400", "403", "404", "405", "413", "500"};
        for (String code : requiredErrors) {
            if (!errorPages.containsKey(code)) {
                throw new IllegalArgumentException("Missing error page for: " + code);
            }
        }
        
        if (routes == null || routes.isEmpty()) {
            throw new IllegalArgumentException("At least one route is required");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServerConfig{\n");
        sb.append("  host=").append(server.getHost()).append("\n");
        sb.append("  ports=").append(server.getPorts()).append("\n");
        sb.append("  maxBodySize=").append(content.getMaxClientBodySize()).append("\n");
        sb.append("  routes=").append(routes.size()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}