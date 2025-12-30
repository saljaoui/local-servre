package config;

import config.model.WebServerConfig;
import routing.model.Route;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Main entry point for loading and parsing web server configuration
 * Delegates low-level JSON parsing to JsonParser and type conversion to
 * ValueParsers
 */
public class WebConfigLoader {
    private static final String FILE_PATH = "./config/config.json";

    public static WebServerConfig load() {
        try {
            String content = Files.readString(Path.of(FILE_PATH));
            WebServerConfig config = parseConfig(content);
            config.validate();
            return parseConfig(content);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            System.exit(1);
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
        Map<String, String> sections = JsonParser.splitTopLevel(json);

        if (sections.containsKey("timeouts")) {
            config.setTimeouts(parseTimeouts(sections.get("timeouts")));
        }

        if (sections.containsKey("servers")) {
            config.setServers(parseServers(sections.get("servers")));
        }

        return config;
    }

    private static WebServerConfig.Timeouts parseTimeouts(String json) {
        WebServerConfig.Timeouts timeouts = new WebServerConfig.Timeouts();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("headerMillis")) {
            timeouts.setHeaderMillis(ValueParsers.parseInt(fields.get("headerMillis")));
        }
        if (fields.containsKey("bodyMillis")) {
            timeouts.setBodyMillis(ValueParsers.parseInt(fields.get("bodyMillis")));
        }
        if (fields.containsKey("keepAliveMillis")) {
            timeouts.setKeepAliveMillis(ValueParsers.parseInt(fields.get("keepAliveMillis")));
        }

        return timeouts;
    }

    private static List<WebServerConfig.ServerBlock> parseServers(String json) {
        List<WebServerConfig.ServerBlock> servers = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();

        List<String> serverJsons = JsonParser.splitArray(json);

        for (String serverJson : serverJsons) {
            servers.add(parseServerBlock(serverJson));
        }

        return servers;
    }

    private static WebServerConfig.ServerBlock parseServerBlock(String json) {
        WebServerConfig.ServerBlock server = new WebServerConfig.ServerBlock();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("name")) {
            server.setName(ValueParsers.parseString(fields.get("name")));
        }
        if (fields.containsKey("listen")) {
            server.setListen(parseListenAddress(fields.get("listen")));
        }
        if (fields.containsKey("serverNames")) {
            server.setServerNames(ValueParsers.parseStringArray(fields.get("serverNames")));
        }
        if (fields.containsKey("root")) {
            server.setRoot(ValueParsers.parseString(fields.get("root")));
        }
        if (fields.containsKey("clientMaxBodyBytes")) {
            server.setClientMaxBodyBytes(ValueParsers.parseLong(fields.get("clientMaxBodyBytes")));
        }
        if (fields.containsKey("errorPages")) {
            server.setErrorPages(ValueParsers.parseStringMap(fields.get("errorPages")));
        }
        if (fields.containsKey("routes")) {
            server.setRoutes(parseRoutes(fields.get("routes")));
        }

        return server;
    }

    private static WebServerConfig.ListenAddress parseListenAddress(String json) {
        WebServerConfig.ListenAddress addr = new WebServerConfig.ListenAddress();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("host")) {
            addr.setHost(ValueParsers.parseString(fields.get("host")));
        }
        if (fields.containsKey("port")) {
            addr.setPort(ValueParsers.parseInt(fields.get("port")));
        }
        if (fields.containsKey("default")) {
            addr.setDefault(ValueParsers.parseBoolean(fields.get("default")));
        }

        return addr;
    }

    private static List<Route> parseRoutes(String json) {
        List<Route> routes = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim();

        List<String> routeJsons = JsonParser.splitArray(json);

        for (String routeJson : routeJsons) {
            routes.add(parseRoute(routeJson));
        }

        return routes;
    }

    private static Route parseRoute(String json) {
        Route route = new Route();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("path")) {
            route.setPath(ValueParsers.parseString(fields.get("path")));
        }
        if (fields.containsKey("methods")) {
            route.setMethods(ValueParsers.parseStringArray(fields.get("methods")));
        }
        if (fields.containsKey("root")) {
            route.setRoot(ValueParsers.parseString(fields.get("root")));
        }
        if (fields.containsKey("index")) {
            route.setIndex(ValueParsers.parseString(fields.get("index")));
        }
        if (fields.containsKey("autoIndex")) {
            route.setAutoIndex(ValueParsers.parseBoolean(fields.get("autoIndex")));
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

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("enabled")) {
            upload.setEnabled(ValueParsers.parseBoolean(fields.get("enabled")));
        }
        if (fields.containsKey("dir")) {
            upload.setDir(ValueParsers.parseString(fields.get("dir")));
        }
        if (fields.containsKey("fileField")) {
            upload.setFileField(ValueParsers.parseString(fields.get("fileField")));
        }

        return upload;
    }

    private static WebServerConfig.Cgi parseCgi(String json) {
        WebServerConfig.Cgi cgi = new WebServerConfig.Cgi();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("enabled")) {
            cgi.setEnabled(ValueParsers.parseBoolean(fields.get("enabled")));
        }
        if (fields.containsKey("binDir")) {
            cgi.setBinDir(ValueParsers.parseString(fields.get("binDir")));
        }
        if (fields.containsKey("byExtension")) {
            cgi.setByExtension(ValueParsers.parseStringMap(fields.get("byExtension")));
        }

        return cgi;
    }

    private static WebServerConfig.Redirect parseRedirect(String json) {
        WebServerConfig.Redirect redirect = new WebServerConfig.Redirect();
        json = json.substring(1, json.length() - 1).trim();

        Map<String, String> fields = JsonParser.splitTopLevel(json);

        if (fields.containsKey("status")) {
            redirect.setStatus(ValueParsers.parseInt(fields.get("status")));
        }
        if (fields.containsKey("to")) {
            redirect.setTo(ValueParsers.parseString(fields.get("to")));
        }

        return redirect;
    }
}