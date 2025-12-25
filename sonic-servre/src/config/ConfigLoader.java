package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class ConfigLoader {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static ServerConfig load(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();

        ServerConfig config = new ServerConfig();

        // Parse server settings
        if (json.has("server")) {
            config.setServer(parseServerSettings(json.getAsJsonObject("server")));
        }

        // Parse content settings
        if (json.has("content")) {
            config.setContent(parseContentSettings(json.getAsJsonObject("content")));
        }

        // Parse error pages
        if (json.has("errorPages")) {
            config.setErrorPages(parseErrorPages(json.getAsJsonObject("errorPages")));
        }

        // Parse CGI settings
        if (json.has("cgi")) {
            config.setCgi(parseCgiSettings(json.getAsJsonObject("cgi")));
        }

        // Parse session settings
        if (json.has("sessions")) {
            config.setSessions(parseSessionSettings(json.getAsJsonObject("sessions")));
        }

        // Parse routes
        if (json.has("routes")) {
            config.setRoutes(parseRoutes(json.getAsJsonArray("routes")));
        }

        config.validate();
        return config;
    }

    private static ServerConfig.ServerSettings parseServerSettings(JsonObject json) {
        ServerConfig.ServerSettings settings = new ServerConfig.ServerSettings();

        if (json.has("host")) {
            settings.setHost(json.get("host").getAsString());
        }

        if (json.has("ports")) {
            JsonArray portsArray = json.getAsJsonArray("ports");
            List<Integer> ports = new ArrayList<>();
            for (JsonElement element : portsArray) {
                ports.add(element.getAsInt());
            }
            settings.setPorts(ports);
        }

        if (json.has("defaultPort")) {
            settings.setDefaultPort(json.get("defaultPort").getAsInt());
        }

        if (json.has("timeout")) {
            settings.setTimeout(json.get("timeout").getAsInt());
        }

        return settings;
    }

    private static ServerConfig.ContentSettings parseContentSettings(JsonObject json) {
        ServerConfig.ContentSettings settings = new ServerConfig.ContentSettings();

        if (json.has("maxClientBodySize")) {
            settings.setMaxClientBodySize(json.get("maxClientBodySize").getAsLong());
        }

        if (json.has("defaultIndexFile")) {
            settings.setDefaultIndexFile(json.get("defaultIndexFile").getAsString());
        }

        if (json.has("allowDirectoryListing")) {
            settings.setAllowDirectoryListing(json.get("allowDirectoryListing").getAsBoolean());
        }

        return settings;
    }

    private static Map<String, String> parseErrorPages(JsonObject json) {
        Map<String, String> errorPages = new HashMap<>();
        for (String key : json.keySet()) {
            errorPages.put(key, json.get(key).getAsString());
        }
        return errorPages;
    }

    private static ServerConfig.CgiSettings parseCgiSettings(JsonObject json) {
        ServerConfig.CgiSettings settings = new ServerConfig.CgiSettings();

        if (json.has("enabled")) {
            settings.setEnabled(json.get("enabled").getAsBoolean());
        }

        if (json.has("extensions")) {
            JsonObject extJson = json.getAsJsonObject("extensions");
            Map<String, String> extensions = new HashMap<>();
            for (String key : extJson.keySet()) {
                extensions.put(key, extJson.get(key).getAsString());
            }
            settings.setExtensions(extensions);
        }

        return settings;
    }

    private static ServerConfig.SessionSettings parseSessionSettings(JsonObject json) {
        ServerConfig.SessionSettings settings = new ServerConfig.SessionSettings();

        if (json.has("enabled")) {
            settings.setEnabled(json.get("enabled").getAsBoolean());
        }

        if (json.has("cookieName")) {
            settings.setCookieName(json.get("cookieName").getAsString());
        }

        return settings;
    }

    private static List<ServerConfig.Route> parseRoutes(JsonArray array) {
        List<ServerConfig.Route> routes = new ArrayList<>();

        for (JsonElement element : array) {
            JsonObject routeJson = element.getAsJsonObject();
            ServerConfig.Route route = new ServerConfig.Route();

            if (routeJson.has("path")) {
                route.setPath(routeJson.get("path").getAsString());
            }

            if (routeJson.has("root")) {
                route.setRoot(routeJson.get("root").getAsString());
            }

            if (routeJson.has("methods")) {
                JsonArray methodsArray = routeJson.getAsJsonArray("methods");
                List<String> methods = new ArrayList<>();
                for (JsonElement method : methodsArray) {
                    methods.add(method.getAsString());
                }
                route.setMethods(methods);
            }

            if (routeJson.has("redirect")) {
                route.setRedirect(routeJson.get("redirect").getAsString());
            }

            if (routeJson.has("statusCode")) {
                route.setStatusCode(routeJson.get("statusCode").getAsInt());
            }

            if (routeJson.has("cgiEnabled")) {
                route.setCgiEnabled(routeJson.get("cgiEnabled").getAsBoolean());
            }

            if (routeJson.has("allowDirectoryListing")) {
                route.setAllowDirectoryListing(routeJson.get("allowDirectoryListing").getAsBoolean());
            }

            routes.add(route);
        }

        return routes;
    }
}