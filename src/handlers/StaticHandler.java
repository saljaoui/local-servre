package handlers;

import config.model.WebServerConfig;
import config.model.WebServerConfig.ServerBlock;
// import config.model.WebServerConfig.Route;
import http.model.HttpRequest;
import http.model.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;

import routing.model.Route;

public class StaticHandler {

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        String method = request.getMethod();
        
        // 1. Resolve Path Logic
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);

        // ==========================================
        // DYNAMIC METHOD HANDLING
        // ==========================================
        switch (method) {
            case "GET":
                return handleGet(filePath,request, route);
            
            case "POST":
                return handlePost(filePath, request, route);
            
            case "DELETE":
                return handleDelete(filePath);
            
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
    private HttpResponse handleGet(Path filePath,HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();
        try {
            if (filePath == null) {
                response.setStatusCode(403); // Security issue
                response.setStatusMessage("Forbidden");
                return response;
            }

            // CHECK: Is it a Directory?
            if (Files.isDirectory(filePath)) {
                // If autoIndex is ON, show list of files
                if (route.isAutoIndex()) {
                    return generateAutoIndex(filePath, request.getPath());
                }
                
                // If autoIndex is OFF, try to serve index.html
                String indexFile = (route.getIndex() != null) ? route.getIndex() : "index.html";
                Path indexPath = filePath.resolve(indexFile);
                
                if (Files.exists(indexPath) && !Files.isDirectory(indexPath)) {
                    return serveFile(indexPath);
                } else {
                    response.setStatusCode(404);
                    response.setStatusMessage("Not Found");
                    response.setBody("<h1>404 No Index Found</h1>".getBytes());
                    response.addHeader("Content-Type", "text/html");
                    return response;
                }
            }

            // Regular File
            if (Files.exists(filePath)) {
                return serveFile(filePath);
            } else {
                response.setStatusCode(404);
                response.setStatusMessage("Not Found");
                response.setBody("<h1>404 File Not Found</h1>".getBytes());
                response.addHeader("Content-Type", "text/html");
            }
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setStatusMessage("Internal Error");
            response.setBody("<h1>500 Error reading file</h1>".getBytes());
            response.addHeader("Content-Type", "text/html");
        }
        return response;
    }

    // --- 2. HANDLE POST (UPLOAD/CREATE FILE) ---
    private HttpResponse handlePost(Path filePath, HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();

        // Check if Upload is enabled in Config
        if (route.isUploadEnabled()) {
            try {
                // CRITICAL: Use MultipartParser to get clean content (Image/Video data)
                // Without this, headers get saved into file and it breaks.
                byte[] body = request.getBody(); 
                
                // IF you implemented MultipartParser from previous step, use it here:
                // byte[] body = MultipartParser.extractFileContent(request);
                
                if (body == null) body = new byte[0];

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

    // --- 3. HANDLE DELETE (REMOVE FILE) ---
    private HttpResponse handleDelete(Path filePath) {
        HttpResponse response = new HttpResponse();
        try {
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                Files.delete(filePath);
                response.setStatusCode(200);
                response.setStatusMessage("OK");
                response.setBody("File Deleted Successfully.".getBytes());
                response.addHeader("Content-Type", "text/plain");
            } else {
                response.setStatusCode(404);
                response.setStatusMessage("Not Found");
                response.setBody("File not found, cannot delete.".getBytes());
                response.addHeader("Content-Type", "text/plain");
            }
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setStatusMessage("Error Deleting");
            response.setBody("Failed to delete file.".getBytes());
            response.addHeader("Content-Type", "text/plain");
        }
        return response;
    }

    // HELPER: Serve file bytes
    private HttpResponse serveFile(Path filePath) throws IOException {
        HttpResponse response = new HttpResponse();
        byte[] content = Files.readAllBytes(filePath);
        String contentType = getContentType(filePath);
        
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.addHeader("Content-Type", contentType);
        response.setBody(content);
        return response;
    }

    // HELPER: Generate HTML list of files
    private HttpResponse generateAutoIndex(Path dirPath, String requestPath) throws IOException {
        HttpResponse response = new HttpResponse();
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html><html><head><title>Index of ").append(requestPath).append("</title>");
        html.append("<style>body { font-family: sans-serif; padding: 20px; } ");
        html.append("a { color: #007BFF; text-decoration: none; } ");
        html.append("a:hover { text-decoration: underline; }</style></head><body>");
        html.append("<h1>Index of ").append(requestPath).append("</h1>");
        
        // Upload Form (Always present at top)
        html.append("<hr/><h3>Upload a File</h3>");
        html.append("<form action=\"").append(requestPath).append("\" method=\"POST\" enctype=\"multipart/form-data\">");
        html.append("<input type=\"file\" name=\"file\" required>");
        html.append("<button type=\"submit\">Upload</button></form>");
        html.append("<hr/>");

        // List Files
        html.append("<ul>");
        if (!dirPath.toFile().exists()) {
            html.append("<li>Directory is empty.</li>");
        } else {
            try (var stream = Files.list(dirPath)) {
                stream.forEach(file -> {
                    String fileName = file.getFileName().toString();
                    html.append("<li>");
                    // Link to the file
                    html.append("<a href=\"").append(requestPath).append("/").append(fileName).append("\">");
                    html.append(fileName).append("</a>");
                    html.append("</li>");
                });
            }
        }
        html.append("</ul>");
        html.append("</body></html>");

        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.addHeader("Content-Type", "text/html");
        response.setBody(html.toString().getBytes());
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
            
            if (path.isEmpty()) path = "/"; 
            
            // 2. Handle Index File
            // Only append index.html if the path ends with "/" (Requesting directory)
            // OR if we are looking for the root specifically.
            // If we are looking for a specific file (img.jpg), do NOT append index.html!
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
    // HELPER: Get MIME type (Added Video support)
    private String getContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".mp4")) return "video/mp4"; // VIDEO
        if (fileName.endsWith(".webm")) return "video/webm"; // VIDEO
        if (fileName.endsWith(".avi")) return "video/x-msvideo"; // VIDEO
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}