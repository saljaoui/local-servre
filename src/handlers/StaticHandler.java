package handlers;

import config.model.WebServerConfig.ServerBlock;
import http.model.HttpRequest;
import http.model.HttpResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import routing.model.Route;
import util.SonicLogger;

public class StaticHandler {
    private static final SonicLogger logger = SonicLogger.getLogger(StaticHandler.class);

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        String method = request.getMethod();
        // 1. Resolve Path Logic
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);
        switch (method) {
            case "GET": {
                return handleGet(filePath, request, route);
            }
            case "POST":
                return handlePost(filePath, request, route);
            case "DELETE":
                // return handleDelete(filePath);
            default:
                HttpResponse err = new HttpResponse();
                err.setStatusCode(501);
                err.setStatusMessage("Not Implemented");
                err.setBody(("Method " + method + " is not implemented for this resource.").getBytes());
                err.addHeader("Content-Type", "text/plain");
                return err;
        }
    }

    // --- 1. HANDLE GET (READ FILE) ---
    private HttpResponse handleGet(Path filePath, HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();
        if (filePath == null) {
            response.setStatusCode(404);
            response.setBody("404 Not Found".getBytes());
            return response;
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            response.setStatusCode(404);
            response.setBody("404 Not Found".getBytes());
            return response;
        }
        if (file.isDirectory()) {
            if (route.isAutoIndex()) {
                // Generate directory listing
                String html = generateDirectoryListing(file, request.getPath());
                response.setStatusCode(200);
                response.setBody(html.getBytes());
                response.addHeader("Content-Type", "text/html");
                return response;
            } else {
                // Use index file if exists
                File indexFile = new File(file, route.getIndex() != null ? route.getIndex() : "index.html");
                if (indexFile.exists() && indexFile.isFile()) {
                    byte[] content;
                    try {
                        content = Files.readAllBytes(indexFile.toPath());
                        response.setStatusCode(200);
                        response.setBody(content);
                        response.addHeader("Content-Type", util.MimeTypes.getMimeType(indexFile.getName()));
                        return response;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } else {
                    response.setStatusCode(403);
                    response.setBody("Forbidden: Directory listing not allowed".getBytes());
                    response.addHeader("Content-Type", "text/plain");
                    return response;
                }
            }
        }


        return response;
    }

    // --- 2. HANDLE POST (UPLOAD/CREATE FILE) ---
    private HttpResponse handlePost(Path filePath, HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();

        // Check if Upload is enabled in Config
        if (route.isUploadEnabled()) {
            try {
                byte[] body = request.getBody();
                System.out
                        .println("[DEBUG] Uploading file to: " + filePath + ", Body length: " + Arrays.toString(body));
                if (body == null)
                    body = new byte[0];

                // Write body to file
                Files.write(filePath, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                response.setStatusCode(201);
                response.setStatusMessage("Created");
                response.setBody(("File Uploaded Successfully: " + filePath.getFileName()).getBytes());
                response.addHeader("Content-Type", "text/plain");
            } catch (IOException e) {
                e.printStackTrace();
                response.setStatusCode(500);
                response.setStatusMessage("Error Saving");
                response.setBody("Failed to save file.".getBytes());
                response.addHeader("Content-Type", "text/plain");
            }
        } else {
            response.setStatusCode(403);
            response.setStatusMessage("Forbidden");
            response.setBody("Uploads not allowed on this path.".getBytes());
            response.addHeader("Content-Type", "text/plain");
        }
        return response;
    }

    // HELPER: Resolve path safely
    private Path resolveFilePath(String root, String requestPath, Route route) {
        try {
            String routePath = route.getPath(); // e.g., "/uploads"
            String relative = requestPath;

            // Strip the route prefix
            if (relative.startsWith(routePath)) {
                relative = relative.substring(routePath.length());
            }

            // If empty, show index
            if (relative.isEmpty() || relative.equals("/")) {
                relative = route.getIndex() != null ? "/" + route.getIndex() : "/index.html";
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
                html.append("<li><a href=\"")
                        .append(routePath)
                        .append(routePath.endsWith("/") ? "" : "/")
                        .append(name)
                        .append("\">")
                        .append(name)
                        .append("</a></li>");
            }
        }

        html.append("</ul></body></html>");
        return html.toString();
    }

}
