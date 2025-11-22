package config;

import java.util.List;
import java.util.Map;

public class ServerConfig {
    private String host;
    private List<Integer> ports;
    private int timeout;
    private long clientMaxBodySize;
    private Map<Integer, String> errorPages;
    private List<RouteConfig> routes;

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public List<Integer> getPorts() { return ports; }
    public void setPorts(List<Integer> ports) { this.ports = ports; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public long getClientMaxBodySize() { return clientMaxBodySize; }
    public void setClientMaxBodySize(long size) { this.clientMaxBodySize = size; }

    public Map<Integer, String> getErrorPages() { return errorPages; }
    public void setErrorPages(Map<Integer, String> errorPages) { this.errorPages = errorPages; }

    public List<RouteConfig> getRoutes() { return routes; }
    public void setRoutes(List<RouteConfig> routes) { this.routes = routes; }

    // Inner class for route configuration
    public static class RouteConfig {
        private String path;
        private String root;
        private String defaultFile;
        private List<String> allowedMethods;
        private boolean directoryListing;
        private String cgiExtension;
        private String cgiPath;
        private String redirectUrl;
        private int redirectCode;

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getRoot() { return root; }
        public void setRoot(String root) { this.root = root; }

        public String getDefaultFile() { return defaultFile; }
        public void setDefaultFile(String defaultFile) { this.defaultFile = defaultFile; }

        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> methods) { this.allowedMethods = methods; }

        public boolean isDirectoryListing() { return directoryListing; }
        public void setDirectoryListing(boolean directoryListing) { this.directoryListing = directoryListing; }

        public String getCgiExtension() { return cgiExtension; }
        public void setCgiExtension(String cgiExtension) { this.cgiExtension = cgiExtension; }

        public String getCgiPath() { return cgiPath; }
        public void setCgiPath(String cgiPath) { this.cgiPath = cgiPath; }

        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

        public int getRedirectCode() { return redirectCode; }
        public void setRedirectCode(int redirectCode) { this.redirectCode = redirectCode; }

        // Helper methods
        public boolean isRedirect() { return redirectUrl != null && !redirectUrl.isEmpty(); }
        public boolean isCgi() { return cgiExtension != null && !cgiExtension.isEmpty(); }
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "host='" + host + '\'' +
                ", ports=" + ports +
                ", timeout=" + timeout +
                ", routes=" + routes.size() +
                '}';
    }
}