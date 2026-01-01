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

        if (!file.exists() || file.isDirectory()) {
            response.setStatusCode(404);
            response.setBody("404 Not Found".getBytes());
            return response;
        }

        try {
            // 1. Read file bytes
            byte[] content = Files.readAllBytes(file.toPath());

            // 2. Set body
            response.setBody(content);
            response.setStatusCode(200);
            response.setStatusMessage("OK");

            // 3. Detect MIME type from extension
            String mimeType = util.MimeTypes.getMimeType(file.getName());
            response.addHeader("Content-Type", mimeType);

            // Optional: Content-Length
            response.addHeader("Content-Length", String.valueOf(content.length));

        } catch (IOException e) {
            e.printStackTrace();
            response.setStatusCode(500);
            response.setBody("Internal Server Error".getBytes());
            response.addHeader("Content-Type", "text/plain");
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

            // Strip route prefix if needed
            String routePath = route.getPath(); // e.g., "/uploads"
            if (path.startsWith(routePath)) {
                path = path.substring(routePath.length());
            }

            if (path.isEmpty() || path.equals("/")) {
                // Optional: default file
                path = "/" + (route.getIndex() != null ? route.getIndex() : "index.html");
            }

            // Normalize path to avoid ../ attacks
            Path resolved = Paths.get(root, path).normalize();

            if (!resolved.startsWith(Paths.get(root).normalize())) {
                return null; // security: outside root
            }

            return resolved;
        } catch (Exception e) {
            return null;
        }
    }

}
