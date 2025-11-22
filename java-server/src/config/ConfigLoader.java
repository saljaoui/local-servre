package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    public static ServerConfig load(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));
        return parse(content);
    }

    private static ServerConfig parse(String json) {
        ServerConfig config = new ServerConfig();
        
        // Remove whitespace and newlines for easier parsing
        json = json.trim();
        
        // Parse host
        config.setHost(extractString(json, "host"));
        
        // Parse ports
        config.setPorts(extractIntArray(json, "ports"));
        
        // Parse timeout
        config.setTimeout(extractInt(json, "timeout"));
        
        // Parse client_max_body_size
        config.setClientMaxBodySize(extractLong(json, "client_max_body_size"));
        
        // Parse error_pages
        config.setErrorPages(extractErrorPages(json));
        
        // Parse routes
        config.setRoutes(extractRoutes(json));
        
        // Validate config
        validate(config);
        
        return config;
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static long extractLong(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }

    private static List<Integer> extractIntArray(String json, String key) {
        List<Integer> result = new ArrayList<>();
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            String[] nums = m.group(1).split(",");
            for (String num : nums) {
                result.add(Integer.parseInt(num.trim()));
            }
        }
        return result;
    }

    private static Map<Integer, String> extractErrorPages(String json) {
        Map<Integer, String> errorPages = new HashMap<>();
        String pattern = "\"error_pages\"\\s*:\\s*\\{([^}]+)\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            String content = m.group(1);
            java.util.regex.Pattern ep = java.util.regex.Pattern.compile("\"(\\d+)\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher em = ep.matcher(content);
            while (em.find()) {
                errorPages.put(Integer.parseInt(em.group(1)), em.group(2));
            }
        }
        return errorPages;
    }

    private static List<ServerConfig.RouteConfig> extractRoutes(String json) {
        List<ServerConfig.RouteConfig> routes = new ArrayList<>();
        
        // Find routes array
        int routesStart = json.indexOf("\"routes\"");
        if (routesStart == -1) return routes;
        
        int arrayStart = json.indexOf("[", routesStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        String routesJson = json.substring(arrayStart + 1, arrayEnd);
        
        // Split into individual route objects
        int braceCount = 0;
        int routeStart = -1;
        for (int i = 0; i < routesJson.length(); i++) {
            char c = routesJson.charAt(i);
            if (c == '{') {
                if (braceCount == 0) routeStart = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && routeStart != -1) {
                    String routeJson = routesJson.substring(routeStart, i + 1);
                    routes.add(parseRoute(routeJson));
                    routeStart = -1;
                }
            }
        }
        return routes;
    }

    private static ServerConfig.RouteConfig parseRoute(String json) {
        ServerConfig.RouteConfig route = new ServerConfig.RouteConfig();
        
        route.setPath(extractString(json, "path"));
        route.setRoot(extractString(json, "root"));
        route.setDefaultFile(extractString(json, "default_file"));
        route.setCgiExtension(extractString(json, "cgi_extension"));
        route.setCgiPath(extractString(json, "cgi_path"));
        route.setRedirectUrl(extractString(json, "redirect_url"));
        route.setRedirectCode(extractInt(json, "redirect_code"));
        
        // Parse directory_listing boolean
        if (json.contains("\"directory_listing\"")) {
            route.setDirectoryListing(json.contains("\"directory_listing\": true") || 
                                       json.contains("\"directory_listing\":true"));
        }
        
        // Parse allowed_methods array
        List<String> methods = new ArrayList<>();
        String pattern = "\"allowed_methods\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            String[] parts = m.group(1).split(",");
            for (String part : parts) {
                methods.add(part.trim().replace("\"", ""));
            }
        }
        route.setAllowedMethods(methods);
        
        return route;
    }

    private static int findMatchingBracket(String json, int start) {
        int count = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') count++;
            else if (json.charAt(i) == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private static void validate(ServerConfig config) {
        if (config.getHost() == null || config.getHost().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }
        if (config.getPorts() == null || config.getPorts().isEmpty()) {
            throw new IllegalArgumentException("At least one port is required");
        }
        if (config.getTimeout() <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (config.getRoutes() == null || config.getRoutes().isEmpty()) {
            throw new IllegalArgumentException("At least one route is required");
        }
        
        for (ServerConfig.RouteConfig route : config.getRoutes()) {
            if (route.getPath() == null || route.getPath().isEmpty()) {
                throw new IllegalArgumentException("Route path is required");
            }
        }
        
        System.out.println("[ConfigLoader] Configuration validated successfully");
    }
}