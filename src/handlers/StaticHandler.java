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
        
        // Get root folder
        String rootFolder = (route.getRoot() != null) ? route.getRoot() : server.getRoot();
        
        // Resolve file path
        Path filePath = resolveFilePath(rootFolder, request.getPath(), route);
        
        if (filePath == null) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }
        
        // Handle by method
        switch (method) {
            case "GET":
                return handleGet(filePath, request, route, server);
            case "POST":
                return handlePost(filePath, request, route, server);
            case "DELETE":
                return handleDelete(filePath, server);
            default:
                return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // ========== GET REQUEST (READ FILE OR LIST DIRECTORY) ==========
    private HttpResponse handleGet(Path filePath, HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        File file = filePath.toFile();
        
        // Check if file exists
        if (!file.exists()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        
        // Is it a directory?
        if (file.isDirectory()) {
            return handleDirectory(file, request.getPath(), route, server);
        }
        
        // It's a regular file - serve it
        return serveFile(file, server);
    }

    // Handle directory based on autoIndex
    private HttpResponse handleDirectory(File directory, String requestPath, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        // Check autoIndex setting
        if (route.isAutoIndex()) {
            // ✅ Generate directory listing
            String html = generateDirectoryListing(directory, requestPath);
            response.setStatus(HttpStatus.OK);
            response.setBody(html.getBytes());
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            return response;
        } else {
            // ❌ autoIndex is false - try to serve index file
            String indexFileName = (route.getIndex() != null && !route.getIndex().isEmpty()) 
                ? route.getIndex() 
                : "index.html";
            
            File indexFile = new File(directory, indexFileName);
            
            if (indexFile.exists() && indexFile.isFile()) {
                return serveFile(indexFile, server);
            } else {
                // No index file found - return 403 Forbidden
                return errorHandler.handle(server, HttpStatus.FORBIDDEN);
            }
        }
    }

    // Serve a regular file
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

    // ========== POST REQUEST (UPLOAD FILE) ==========
    private HttpResponse handlePost(Path filePath, HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        // Check if upload is enabled
        if (!route.isUploadEnabled()) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }
        
        try {
            byte[] body = request.getBody();
            if (body == null) {
                body = new byte[0];
            }
            
            // Check body size limit
            long maxSize = server.getClientMaxBodyBytes();
            if (body.length > maxSize) {
                return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
            }
            
            // Write file
            Files.write(filePath, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            response.setStatus(HttpStatus.CREATED);
            response.setBody(("File uploaded: " + filePath.getFileName()).getBytes());
            response.addHeader("Content-Type", "text/plain");
            
        } catch (IOException e) {
            logger.error("Error uploading file: " + filePath, e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return response;
    }

    // ========== DELETE REQUEST (DELETE FILE) ==========
    private HttpResponse handleDelete(Path filePath, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        File file = filePath.toFile();
        
        // Check if file exists
        if (!file.exists()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        
        // Don't allow deleting directories
        if (file.isDirectory()) {
            response.setStatusCode(403);
            response.setBody("Cannot delete directory".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Delete the file
        try {
            Files.delete(filePath);
            response.setStatus(HttpStatus.NO_CONTENT);
        } catch (IOException e) {
            logger.error("Error deleting file: " + filePath, e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return response;
    }

    // ========== HELPER: RESOLVE FILE PATH SAFELY ==========
    private Path resolveFilePath(String root, String requestPath, Route route) {
        try {
            String routePath = route.getPath(); // e.g., "/uploads"
            
            // Remove route prefix from request path
            String relativePath = requestPath;
            if (relativePath.startsWith(routePath)) {
                relativePath = relativePath.substring(routePath.length());
            }
            
            // Handle empty path
            if (relativePath.isEmpty() || relativePath.equals("/")) {
                relativePath = "/";
            }
            
            // Build full path
            Path fullPath = Paths.get(root, relativePath).normalize();
            
            // Security check: prevent directory traversal
            Path rootPath = Paths.get(root).normalize();
            if (!fullPath.startsWith(rootPath)) {
                logger.warn("Security: Path traversal attempt blocked: " + requestPath);
                return null;
            }
            
            return fullPath;
            
        } catch (Exception e) {
            logger.error("Error resolving path: " + requestPath, e);
            return null;
        }
    }

    // ========== GENERATE DIRECTORY LISTING HTML ==========
    private String generateDirectoryListing(File directory, String requestPath) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Index of ").append(requestPath).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #0066cc; padding-bottom: 10px; }\n");
        html.append("ul { list-style: none; padding: 0; background: white; border-radius: 5px; }\n");
        html.append("li { padding: 12px; border-bottom: 1px solid #eee; }\n");
        html.append("li:hover { background: #f9f9f9; }\n");
        html.append("a { color: #0066cc; text-decoration: none; }\n");
        html.append("a:hover { text-decoration: underline; }\n");
        html.append(".icon { margin-right: 8px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<h1>📁 Index of ").append(requestPath).append("</h1>\n");
        html.append("<ul>\n");
        
        // Add parent directory link if not root
        if (!requestPath.equals("/")) {
            html.append("<li><a href=\"..\"><span class=\"icon\">⬆️</span>Parent Directory</a></li>\n");
        }
        
        // List files and directories
        File[] files = directory.listFiles();
        if (files != null) {
            // Sort: directories first, then files alphabetically
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            
            for (File file : files) {
                String name = file.getName();
                String icon = file.isDirectory() ? "📁" : "📄";
                
                // Build href
                String href = requestPath.endsWith("/") ? requestPath : requestPath + "/";
                href += name;
                if (file.isDirectory()) {
                    href += "/";
                }
                
                html.append("<li>");
                html.append("<a href=\"").append(href).append("\">");
                html.append("<span class=\"icon\">").append(icon).append("</span>");
                html.append(name);
                if (file.isDirectory()) html.append("/");
                html.append("</a>");
                html.append("</li>\n");
            }
        } else {
            html.append("<li>Empty directory</li>\n");
        }
        
        html.append("</ul>\n");
        html.append("</body>\n</html>");
        
        return html.toString();
    }
}