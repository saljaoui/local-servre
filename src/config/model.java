package config;

import java.util.*;

// Root configuration class
class model {
    public Timeouts timeouts;
    public List<Server> servers;
}

// Timeouts class
class Timeouts {
    public int headerMillis;
    public int bodyMillis;
    public int keepAliveMillis;
}

// Server class
class Server {
    public String name;
    public List<Listen> listen;
    public List<String> serverNames;
    public String root;
    public int clientMaxBodyBytes;
    public Map<String, String> errorPages;
    public List<Route> routes;
}

// Listen class
class Listen {
    public String host;
    public int port;
    public boolean defaultVal; // "default" is a keyword, so use defaultVal
}

// Route class
class Route {
    public String path;
    public List<String> methods;
    public String root;
    public String index;
    public boolean autoIndex;
    public Upload upload;
    public CGI cgi;
    public Redirect redirect;
}

// Upload class
class Upload {
    public boolean enabled;
    public String dir;
    public String fileField;
}

// CGI class
class CGI {
    public boolean enabled;
    public String binDir;
    public Map<String, String> byExtension;
}

// Redirect class
class Redirect {
    public int status;
    public String to;
}
