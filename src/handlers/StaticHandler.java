package handlers;

import config.model.WebServerConfig.ServerBlock;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import routing.model.Route;
import util.SonicLogger;

public class StaticHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(StaticHandler.class);
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);

        if (filePath == null) {
            // System.out.println("StaticHandler.handle()"+filePath);
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }
        // System.out.println("StaticHandler.handle()"+request.getMethod());
        return switch (request.getMethod()) {
            case "GET" ->
                handleGet(filePath, request, route, server);
            case "POST" ->
                handlePost(request);
            default ->
                errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
        };
    }

    private HttpResponse handleGet(Path filePath, HttpRequest request, Route route, ServerBlock server) {
        File file = filePath.toFile();
        System.err.println("her " + filePath);
        if (!file.exists()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        if (file.isDirectory()) {
            return handleDirectory(file, request.getPath(), route, server);
        }

        return serveFile(file, server);
    }

    private HttpResponse handleDirectory(File directory, String requestPath, Route route, ServerBlock server) {
        if (route.isAutoIndex()) {
            HttpResponse response = new HttpResponse();
            response.setStatus(HttpStatus.OK);
            response.setBody(generateDirectoryListing(directory, requestPath).getBytes());
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;
        }

        String indexFileName = (route.getIndex() != null && !route.getIndex().isEmpty())
                ? route.getIndex() : "index.html";

        File indexFile = new File(directory, indexFileName);
        if (indexFile.exists() && indexFile.isFile()) {
            return serveFile(indexFile, server);
        }
        return errorHandler.handle(server, HttpStatus.FORBIDDEN);
    }

    private HttpResponse serveFile(File file, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            response.setStatus(HttpStatus.OK);
            response.setBody(content);
            response.addHeader("Content-Type", util.MimeTypes.getMimeType(file.getName()));
            response.addHeader("Content-Length", String.valueOf(content.length));
        } catch (IOException e) {
            logger.error("Error reading file: " + file.getPath(), e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private HttpResponse handlePost(HttpRequest request) {

        HttpResponse response = new HttpResponse();
        String body = new String(request.getBody(), StandardCharsets.UTF_8);
        response.setStatus(HttpStatus.CREATED);
        response.setBody(("Content: " + body).getBytes(StandardCharsets.UTF_8));
        response.addHeader("Content-Type", "text/plain; charset=UTF-8");
        return response;
    }

    private Path resolveFilePath(String root, String requestPath, Route route) {
        try {
            String relativePath = requestPath;
            if (relativePath.startsWith(route.getPath())) {
                relativePath = relativePath.substring(route.getPath().length());
            }

            if (relativePath.isEmpty() || relativePath.equals("/")) {
                relativePath = "/";
            }

            Path fullPath = Paths.get(root, relativePath).normalize();
            Path rootPath = Paths.get(root).normalize();

            if (!fullPath.startsWith(rootPath)) {
                logger.warn("Path traversal blocked: " + requestPath);
                return null;
            }

            return fullPath;
        } catch (Exception e) {
            logger.error("Error resolving path: " + requestPath, e);
            return null;
        }
    }

    private String generateDirectoryListing(File directory, String requestPath) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        html.append("<title>Index of ").append(requestPath).append("</title>");
        html.append("<style>");
        html.append("body{font-family:Arial;margin:40px;background:#f5f5f5}");
        html.append("h1{color:#333;border-bottom:2px solid #da70d6;padding-bottom:10px}");
        html.append("ul{list-style:none;padding:0;background:#fff;border-radius:5px}");
        html.append("li{padding:12px;border-bottom:1px solid #eee}");
        html.append("li:hover{background:#f9f9f9}");
        html.append("a{color:#8a2be2;text-decoration:none}");
        html.append("a:hover{text-decoration:underline}");
        html.append("</style></head><body>");
        html.append("<h1>📁 Index of ").append(requestPath).append("</h1><ul>");

        if (!requestPath.equals("/")) {
            html.append("<li><a href=\"..\">⬆️ Parent Directory</a></li>");
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String icon = file.isDirectory() ? "📁 " : "📄 ";
                String href = requestPath.endsWith("/") ? requestPath + name : requestPath + "/" + name;

                if (file.isDirectory()) {
                    name += "/";
                    href += "/";
                }

                html.append("<li><a href=\"").append(href).append("\">")
                        .append(icon).append(name).append("</a></li>");
            }
        }

        html.append("</ul></body></html>");
        return html.toString();
    }
}
