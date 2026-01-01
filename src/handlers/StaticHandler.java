package handlers;

import config.model.WebServerConfig.ServerBlock;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import routing.model.Route;
import util.SonicLogger;

public class StaticHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(StaticHandler.class);
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        String method = request.getMethod();
        // 1. Resolve Path Logic
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);
        System.out.println("[DEBUG] Resolved file path: " + filePath);
        if (filePath == null) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }
        switch (method) {
            case "GET":
                return handleGet(filePath, request, route, server);
            case "POST":
                return handlePost(filePath, request, route, server);
            case "DELETE":
                return handleDelete(filePath);
            default:
                return errorHandler.handle(server, HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // --- 1. HANDLE GET (READ FILE) ---
    private HttpResponse handleGet(Path filePath, HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        if (filePath == null) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        if (file.isDirectory()) {
            if (route.isAutoIndex()) {
                String html = generateDirectoryListing(file, request.getPath());
                response.setStatusCode(200);
                response.setBody(html.getBytes());
                response.addHeader("Content-Type", "text/html");
                return response;
            } else {
                File indexFile = new File(file, route.getIndex() != null ? route.getIndex() : "index.html");
                if (indexFile.exists() && indexFile.isFile()) {
                    try {
                        byte[] content = Files.readAllBytes(indexFile.toPath());
                        response.setStatusCode(200);
                        response.setBody(content);
                        response.addHeader("Content-Type", util.MimeTypes.getMimeType(indexFile.getName()));
                        return response;
                    } catch (IOException e) {
                        logger.error("Static index read error: " + indexFile.getPath(), e);
                        return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    return errorHandler.handle(server, HttpStatus.FORBIDDEN);
                }
            }
        }

        try {
            byte[] content = Files.readAllBytes(file.toPath());
            response.setStatusCode(200);
            response.setBody(content);
            response.addHeader("Content-Type", util.MimeTypes.getMimeType(file.getName()));
        } catch (IOException e) {
            logger.error("Static read error: " + file.getPath(), e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    // --- 2. HANDLE POST (UPLOAD/CREATE FILE) ---
    private HttpResponse handlePost(Path filePath, HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();

        // Check if Upload is enabled in Config
        if (route.isUploadEnabled()) {
            try {
                byte[] body = request.getBody();
                if (body == null) {

                    body = new byte[0];
                }

                // Write body to file
                Files.write(filePath, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                response.setStatusCode(201);
                response.setStatusMessage("Created");
                response.setBody(("File Uploaded Successfully: " + filePath.getFileName()).getBytes());
                response.addHeader("Content-Type", "text/plain");
            } catch (IOException e) {
                e.printStackTrace();
                return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }
        return response;
    }

    // HELPER: Resolve path safely
    private Path resolveFilePath(String root, String relative, Route route) {
        try {
            String routePath = route.getPath(); // e.g., "/uploads"

            // Strip the route prefix
            if (relative.startsWith(routePath)) {
                relative = relative.substring(routePath.length());
            }

            if (relative.isEmpty()) {
                relative = "/";
            }
            if (relative.endsWith("/")) {
                String indexFile = (route.getIndex() != null) ? route.getIndex() : "index.html";
                relative = relative + indexFile;
            }

            // Combine root + route + relative file
            Path resolved = Paths.get(root, routePath, relative).normalize();

            System.err.println("[DEBUG] Resolving path: " + resolved);

            // Security check: must be under root
            if (!resolved.startsWith(Paths.get(root).normalize())) {
                return null;
            }

            return resolved;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateDirectoryListing(File dir, String routePath) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Index of ").append(routePath).append("</title></head><body>");
        html.append("<h1>Index of  ").append(routePath).append("</h1><ul>");

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (f.isDirectory()) {
                    name += "/";
                }
                String href = routePath;
                if (!href.endsWith("/"))
                    href += "/";
                href += name;

                html.append("<li><a href=\"")
                        .append(href)
                        .append("\">")
                        .append(name)
                        .append("</a></li>");

            }
        }

        html.append("</ul></body></html>");
        return html.toString();
    }

    private HttpResponse handleDelete(Path filePath) {
        HttpResponse response = new HttpResponse();

        File file = filePath.toFile();

        if (!file.exists()) {
            response.setStatusCode(404);
            response.setBody("File Not Found ".getBytes());
            return response;
        }

        if (file.isDirectory()) {
            response.setStatusCode(403);
            response.setBody("Cannot delete a directory".getBytes());
            return response;
        }
        try {
            Files.delete(filePath);
            response.setStatusCode(204);
            response.setBody("File Deleted Successfully".getBytes());
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody("Internal Server Error".getBytes());
        }
        return response;
    }

}
