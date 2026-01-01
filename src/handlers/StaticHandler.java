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

        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);
        return switch (method) {
            case "GET" -> handleGet(filePath, request, route);
            case "POST" -> handlePost(filePath, request, route);
            case "DELETE" -> handleDelete(filePath);
            default -> {
                HttpResponse err = new HttpResponse();
                err.setStatusCode(501);
                err.setStatusMessage("Not Implemented");
                err.setBody(("Method " + method + " is not implemented for this resource.").getBytes());
                err.addHeader("Content-Type", "text/plain");
                yield err;
            }
        };
    }

    // --- 1. HANDLE GET (READ FILE) ---
    private HttpResponse handleGet(Path filePath, HttpRequest request, Route route) {

        HttpResponse response = new HttpResponse();

        String root = route.getRoot();
        String path = request.getPath();

        if (path.equals("/")) {
            path = "/" + route.getIndex();
        }

        File file = new File(root + path);

        if (!file.exists() || file.isDirectory()) {
            logger.debug("Static 404: " + file.getPath());
            response.setStatusCode(404);
            return response;
        }

        try {
            byte[] content = Files.readAllBytes(file.toPath());
            response.setStatusCode(200);
            response.setBody(content);

        } catch (IOException e) {
            logger.error("Static read error: " + file.getPath(), e);
            response.setStatusCode(500);
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
    private Path resolveFilePath(String root, String uri, Route route) {
        try {
            String path = uri;

            // 1. Strip Route Prefix from URI
            // E.g., Route="/uploads", URI="/uploads/img.jpg" -> path="/img.jpg"
            String routePath = route.getPath();
            if (path.startsWith(routePath)) {
                path = path.substring(routePath.length());
            }

            if (path.isEmpty())
                path = "/";
            if (path.endsWith("/")) {
                String indexFile = (route.getIndex() != null) ? route.getIndex() : "index.html";
                path = path + indexFile;
            }

            Path resolved = Paths.get(root, path).normalize();

            // Security check
            if (!resolved.startsWith(Paths.get(root).normalize())) {
                return null; // Hacking attempt
            }

            return resolved;
        } catch (Exception e) {
            return null;
        }
    }

    private HttpResponse handleDelete(Path filePath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
