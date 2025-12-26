package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ConfigLoader {
    
    public static ServerConfig load(String filePath) {
        try {
            String content = Files.readString(Path.of(filePath));
            return parseConfig(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static ServerConfig parseConfig(String json) {
        ServerConfig config = new ServerConfig();
        json = json.trim();
        
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }
        
        // Remove outer braces
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> sections = splitTopLevel(json);
        
        // Parse server section
        if (sections.containsKey("server")) {
            config.setServer(parseServerSettings(sections.get("server")));
        }
        
        // Parse content section
        if (sections.containsKey("content")) {
            config.setContent(parseContentSettings(sections.get("content")));
        }
        
        // Parse error pages
        if (sections.containsKey("errorPages")) {
            config.setErrorPages(parseStringMap(sections.get("errorPages")));
        }
        
        // Parse CGI settings
        if (sections.containsKey("cgi")) {
            config.setCgi(parseCgiSettings(sections.get("cgi")));
        }
        
        // Parse sessions
        if (sections.containsKey("sessions")) {
            config.setSessions(parseSessionSettings(sections.get("sessions")));
        }
        
        // Parse routes
        if (sections.containsKey("routes")) {
            config.setRoutes(parseRoutes(sections.get("routes")));
        }
        
        return config;
    }
    
    private static Map<String, String> splitTopLevel(String json) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        
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
            
            // Read value (can be object, array, string, number, boolean)
            int valueStart = i;
            i = skipValue(json, i);
            String value = json.substring(valueStart, i).trim();
            
            result.put(key, value);
            
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
            while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}' && json.charAt(i) != ']') {
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
    
    private static ServerConfig.ServerSettings parseServerSettings(String json) {
        ServerConfig.ServerSettings settings = new ServerConfig.ServerSettings();
        json = json.substring(1, json.length() - 1).trim(); // remove braces
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("host")) {
            settings.setHost(parseString(fields.get("host")));
        }
        if (fields.containsKey("ports")) {
            settings.setPorts(parseIntArray(fields.get("ports")));
        }
        if (fields.containsKey("defaultPort")) {
            settings.setDefaultPort(parseInt(fields.get("defaultPort")));
        }
        if (fields.containsKey("timeout")) {
            settings.setTimeout(parseInt(fields.get("timeout")));
        }
        
        return settings;
    }
    
    private static ServerConfig.ContentSettings parseContentSettings(String json) {
        ServerConfig.ContentSettings settings = new ServerConfig.ContentSettings();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("maxClientBodySize")) {
            settings.setMaxClientBodySize(parseLong(fields.get("maxClientBodySize")));
        }
        if (fields.containsKey("defaultIndexFile")) {
            settings.setDefaultIndexFile(parseString(fields.get("defaultIndexFile")));
        }
        if (fields.containsKey("allowDirectoryListing")) {
            settings.setAllowDirectoryListing(parseBoolean(fields.get("allowDirectoryListing")));
        }
        
        return settings;
    }
    
    private static ServerConfig.CgiSettings parseCgiSettings(String json) {
        ServerConfig.CgiSettings settings = new ServerConfig.CgiSettings();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("enabled")) {
            settings.setEnabled(parseBoolean(fields.get("enabled")));
        }
        if (fields.containsKey("extensions")) {
            settings.setExtensions(parseStringMap(fields.get("extensions")));
        }
        
        return settings;
    }
    
    private static ServerConfig.SessionSettings parseSessionSettings(String json) {
        ServerConfig.SessionSettings settings = new ServerConfig.SessionSettings();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("enabled")) {
            settings.setEnabled(parseBoolean(fields.get("enabled")));
        }
        if (fields.containsKey("cookieName")) {
            settings.setCookieName(parseString(fields.get("cookieName")));
        }
        
        return settings;
    }
    
    private static List<ServerConfig.Route> parseRoutes(String json) {
        List<ServerConfig.Route> routes = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim(); // remove brackets
        
        List<String> routeJsons = splitArray(json);
        
        for (String routeJson : routeJsons) {
            routes.add(parseRoute(routeJson));
        }
        
        return routes;
    }
    
    private static ServerConfig.Route parseRoute(String json) {
        ServerConfig.Route route = new ServerConfig.Route();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        
        if (fields.containsKey("path")) {
            route.setPath(parseString(fields.get("path")));
        }
        if (fields.containsKey("root")) {
            route.setRoot(parseString(fields.get("root")));
        }
        if (fields.containsKey("methods")) {
            route.setMethods(parseStringArray(fields.get("methods")));
        }
        if (fields.containsKey("redirect")) {
            route.setRedirect(parseString(fields.get("redirect")));
        }
        if (fields.containsKey("statusCode")) {
            route.setStatusCode(parseInt(fields.get("statusCode")));
        }
        if (fields.containsKey("cgiEnabled")) {
            route.setCgiEnabled(parseBoolean(fields.get("cgiEnabled")));
        }
        if (fields.containsKey("allowDirectoryListing")) {
            route.setAllowDirectoryListing(parseBoolean(fields.get("allowDirectoryListing")));
        }
        
        return route;
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
    
    private static Map<String, String> parseStringMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.substring(1, json.length() - 1).trim();
        
        Map<String, String> fields = splitTopLevel(json);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            map.put(entry.getKey(), parseString(entry.getValue()));
        }
        
        return map;
    }
    
    private static List<String> parseStringArray(String json) {
        List<String> list = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim(); // remove brackets
        
        List<String> items = splitArray(json);
        for (String item : items) {
            list.add(parseString(item));
        }
        
        return list;
    }
    
    private static List<Integer> parseIntArray(String json) {
        List<Integer> list = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();
        
        List<String> items = splitArray(json);
        for (String item : items) {
            list.add(parseInt(item));
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