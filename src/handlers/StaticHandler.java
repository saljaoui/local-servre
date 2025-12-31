package handlers;

 import config.model.WebServerConfig.ServerBlock;
// import config.model.WebServerConfig.Route;
import http.model.HttpRequest;
import http.model.HttpResponse;

import java.io.IOException;
 import java.nio.file.*;

import routing.model.Route;

public class StaticHandler {

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        String method = request.getMethod();

        // 1. Resolve Path Logic
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);
        switch (method) {
            case "GET":
                // return handleGet(filePath, request, route);

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

    // // --- 1. HANDLE GET (READ FILE) ---
    // private HttpResponse handleGet(Path filePath, HttpRequest request, Route
    // route) {
    // HttpResponse response = new HttpResponse();
    // try {
    // if (filePath == null) {
    // response.setStatusCode(403); // Security issue
    // response.setStatusMessage("Forbidden");
    // return response;
    // }

    // // CHECK: Is it a Directory?
    // if (Files.isDirectory(filePath)) {
    // // If autoIndex is ON, show list of files
    // // if (route.isAutoIndex()) {
    // // return generateAutoIndex(filePath, request.getPath());
    // // }

    // // If autoIndex is OFF, try to serve index.html
    // String indexFile = (route.getIndex() != null) ? route.getIndex() :
    // "index.html";
    // Path indexPath = filePath.resolve(indexFile);

    // if (Files.exists(indexPath) && !Files.isDirectory(indexPath)) {
    // return serveFile(indexPath);
    // } else {
    // response.setStatusCode(404);
    // response.setStatusMessage("Not Found");
    // response.setBody("<h1>404 No Index Found</h1>".getBytes());
    // response.addHeader("Content-Type", "text/html");
    // return response;
    // }
    // }

    // // Regular File
    // if (Files.exists(filePath)) {
    // return serveFile(filePath);
    // } else {
    // response.setStatusCode(404);
    // response.setStatusMessage("Not Found");
    // response.setBody("<h1>404 File Not Found</h1>".getBytes());
    // response.addHeader("Content-Type", "text/html");
    // }
    // } catch (IOException e) {
    // response.setStatusCode(500);
    // response.setStatusMessage("Internal Error");
    // response.setBody("<h1>500 Error reading file</h1>".getBytes());
    // response.addHeader("Content-Type", "text/html");
    // }
    // return response;
    // }

    // --- 2. HANDLE POST (UPLOAD/CREATE FILE) ---
    private HttpResponse handlePost(Path filePath, HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();

        // Check if Upload is enabled in Config
        if (route.isUploadEnabled()) {
            try {
                byte[] body = request.getBody();

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
}