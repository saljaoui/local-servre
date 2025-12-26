package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import config.model.WebServerConfig;

public class WebConfigLoader {
    
    public static WebServerConfig load(String filePath) {
        try {            
            String content = Files.readString(Path.of(filePath));            
            return parseConfig(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static WebServerConfig parseConfig(String json) {
        WebServerConfig config = new WebServerConfig();
        json = json.trim();
        
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }
        
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> sections = splitTopLevel(json);
        
        // Parse timeouts
        if (sections.containsKey("timeouts")) {
            config.setTimeouts(parseTimeouts(sections.get("timeouts")));
        }
        
        // Parse servers
        if (sections.containsKey("servers")) {
            config.setServers(parseServers(sections.get("servers")));
        }
        
        return config;
    }
    
    private static WebServerConfig.Timeouts parseTimeouts(String json) {
        WebServerConfig.Timeouts timeouts = new WebServerConfig.Timeouts();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("headerMillis")) {
            int value = parseInt(fields.get("headerMillis"));
            timeouts.setHeaderMillis(value);
        }
        if (fields.containsKey("bodyMillis")) {
            int value = parseInt(fields.get("bodyMillis"));
            timeouts.setBodyMillis(value);
        }
        if (fields.containsKey("keepAliveMillis")) {
            int value = parseInt(fields.get("keepAliveMillis"));
            timeouts.setKeepAliveMillis(value);
        }
        
        return timeouts;
    }
    
    private static List<WebServerConfig.ServerBlock> parseServers(String json) {
        List<WebServerConfig.ServerBlock> servers = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> serverJsons = splitArray(json);
        
        for (int i = 0; i < serverJsons.size(); i++) {
            servers.add(parseServerBlock(serverJsons.get(i)));
        }
        
        return servers;
    }
    
    private static WebServerConfig.ServerBlock parseServerBlock(String json) {
        WebServerConfig.ServerBlock server = new WebServerConfig.ServerBlock();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("name")) {
            String name = parseString(fields.get("name"));
            server.setName(name);
        }
        if (fields.containsKey("listen")) {
            server.setListen(parseListenAddresses(fields.get("listen")));
        }
        if (fields.containsKey("serverNames")) {
            server.setServerNames(parseStringArray(fields.get("serverNames")));
        }
        if (fields.containsKey("root")) {
            String root = parseString(fields.get("root"));
            server.setRoot(root);
        }
        if (fields.containsKey("clientMaxBodyBytes")) {
            long bytes = parseLong(fields.get("clientMaxBodyBytes"));
            server.setClientMaxBodyBytes(bytes);
        }
        if (fields.containsKey("errorPages")) {
            server.setErrorPages(parseStringMap(fields.get("errorPages")));
        }
        if (fields.containsKey("routes")) {
            server.setRoutes(parseRoutes(fields.get("routes")));
        }
        
        return server;
    }
    
    private static List<WebServerConfig.ListenAddress> parseListenAddresses(String json) {
        List<WebServerConfig.ListenAddress> addresses = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> addrJsons = splitArray(json);
        
        for (String addrJson : addrJsons) {
            addresses.add(parseListenAddress(addrJson));
        }
        
        return addresses;
    }
    
    private static WebServerConfig.ListenAddress parseListenAddress(String json) {
        WebServerConfig.ListenAddress addr = new WebServerConfig.ListenAddress();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("host")) {
            String host = parseString(fields.get("host"));
            addr.setHost(host);
        }
        if (fields.containsKey("port")) {
            int port = parseInt(fields.get("port"));
            addr.setPort(port);
        }
        if (fields.containsKey("default")) {
            boolean isDefault = parseBoolean(fields.get("default"));
            addr.setDefault(isDefault);
        }
        
        return addr;
    }
    
    private static List<WebServerConfig.Route> parseRoutes(String json) {
        List<WebServerConfig.Route> routes = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> routeJsons = splitArray(json);
        
        for (int i = 0; i < routeJsons.size(); i++) {
            routes.add(parseRoute(routeJsons.get(i)));
        }
        
        return routes;
    }
    
    private static WebServerConfig.Route parseRoute(String json) {
        WebServerConfig.Route route = new WebServerConfig.Route();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("path")) {
            String path = parseString(fields.get("path"));
            route.setPath(path);
        }
        if (fields.containsKey("methods")) {
            route.setMethods(parseStringArray(fields.get("methods")));
        }
        if (fields.containsKey("root")) {
            String root = parseString(fields.get("root"));
            route.setRoot(root);
        }
        if (fields.containsKey("index")) {
            String index = parseString(fields.get("index"));
            route.setIndex(index);
        }
        if (fields.containsKey("autoIndex")) {
            boolean autoIndex = parseBoolean(fields.get("autoIndex"));
            route.setAutoIndex(autoIndex);
        }
        if (fields.containsKey("upload")) {
            route.setUpload(parseUpload(fields.get("upload")));
        }
        if (fields.containsKey("cgi")) {
            route.setCgi(parseCgi(fields.get("cgi")));
        }
        if (fields.containsKey("redirect")) {
            route.setRedirect(parseRedirect(fields.get("redirect")));
        }
        
        return route;
    }
    
    private static WebServerConfig.Upload parseUpload(String json) {
        WebServerConfig.Upload upload = new WebServerConfig.Upload();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("enabled")) {
            boolean enabled = parseBoolean(fields.get("enabled"));
            upload.setEnabled(enabled);
        }
        if (fields.containsKey("dir")) {
            String dir = parseString(fields.get("dir"));
            upload.setDir(dir);
        }
        if (fields.containsKey("fileField")) {
            String fileField = parseString(fields.get("fileField"));
            upload.setFileField(fileField);
        }
        
        return upload;
    }
    
    private static WebServerConfig.Cgi parseCgi(String json) {
        WebServerConfig.Cgi cgi = new WebServerConfig.Cgi();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("enabled")) {
            boolean enabled = parseBoolean(fields.get("enabled"));
            cgi.setEnabled(enabled);
        }
        if (fields.containsKey("binDir")) {
            String binDir = parseString(fields.get("binDir"));
            cgi.setBinDir(binDir);
        }
        if (fields.containsKey("byExtension")) {
            Map<String, String> extensions = parseStringMap(fields.get("byExtension"));
            cgi.setByExtension(extensions);
        }
        
        return cgi;
    }
    
    private static WebServerConfig.Redirect parseRedirect(String json) {
        WebServerConfig.Redirect redirect = new WebServerConfig.Redirect();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("status")) {
            int status = parseInt(fields.get("status"));
            redirect.setStatus(status);
        }
        if (fields.containsKey("to")) {
            String to = parseString(fields.get("to"));
            redirect.setTo(to);
        }
        
        return redirect;
    }
    
    // ========== CORE JSON PARSING HELPERS ==========
    
    private static Map<String, String> splitTopLevel(String json) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        int pairCount = 0;
        
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            
            // Read key
            if (json.charAt(i) != '"') break;
            i++; // skip opening quote
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // skip closing quote
            
            
            // Skip to colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++; // skip colon
            
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            
            // Read value
            int valueStart = i;
            i = skipValue(json, i);
            String value = json.substring(valueStart, i).trim();
            
            result.put(key, value);
            pairCount++;
            
            // Skip comma
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }
        
        return result;
    }
    
    private static int skipValue(String json, int start) {
        char c = json.charAt(start);
        
        if (c == '{') {
            return skipObject(json, start);
        } else if (c == '[') {
            return skipArray(json, start);
        } else if (c == '"') {
            return skipString(json, start);
        } else {
            // Number, boolean, or null
            int i = start;
            while (i < json.length() && json.charAt(i) != ',' && 
                   json.charAt(i) != '}' && json.charAt(i) != ']') {
                i++;
            }
            return i;
        }
    }
    
    private static int skipObject(String json, int start) {
        int i = start + 1; // skip opening brace
        int depth = 1;
        
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == '"') i = skipString(json, i) - 1;
            i++;
        }
        return i;
    }
    
    private static int skipArray(String json, int start) {
        int i = start + 1; // skip opening bracket
        int depth = 1;
        
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == '"') i = skipString(json, i) - 1;
            else if (c == '{') i = skipObject(json, i) - 1;
            i++;
        }
        return i;
    }
    
    private static int skipString(String json, int start) {
        int i = start + 1; // skip opening quote
        while (i < json.length()) {
            if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                return i + 1;
            }
            i++;
        }
        return i;
    }
    
    private static List<String> splitArray(String json) {
        List<String> items = new ArrayList<>();
        int i = 0;
        
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            
            int start = i;
            i = skipValue(json, i);
            items.add(json.substring(start, i).trim());
            
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }
        
        return items;
    }
    
    // ========== VALUE PARSERS ==========
    
    private static Map<String, String> parseStringMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = parseString(entry.getValue());
            map.put(entry.getKey(), value);
        }
        
        return map;
    }
    
    private static List<String> parseStringArray(String json) {
        List<String> list = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> items = splitArray(json);
        
        for (String item : items) {
            String parsed = parseString(item);
            list.add(parsed);
        }
        
        return list;
    }
    
    private static String parseString(String json) {
        json = json.trim();
        if (json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1);
        }
        return json;
    }
    
    private static int parseInt(String json) {
        return Integer.parseInt(json.trim());
    }
    
    private static long parseLong(String json) {
        return Long.parseLong(json.trim());
    }
    
    private static boolean parseBoolean(String json) {
        return Boolean.parseBoolean(json.trim());
    }
}
