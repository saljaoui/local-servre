package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import config.model.WebServerConfig.ServerBlock;
import routing.model.Route;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
 
public class DeleteHandler {
    
    /**
     * Handle DELETE requests.
     * Deletes the specified file or resource.
     */
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        String path = request.getPath();
        if (path == null || path.isEmpty() || path.equals("/")) {
            response.setStatusCode(400);
            response.setBody("Bad Request: Cannot delete root or empty path".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Determine the file to delete based on route and request path
        String root = route.getRoot();
        if (root == null || root.isEmpty()) {
            root = "www";
        }
        
        // Remove leading slash from path for file operations
        String filePathStr = path.startsWith("/") ? path.substring(1) : path;
        File fileToDelete = new File(root, filePathStr);
        
        // Security check: ensure the file is within the allowed directory
        try {
            String canonicalRoot = new File(root).getCanonicalPath();
            String canonicalFile = fileToDelete.getCanonicalPath();
            
            if (!canonicalFile.startsWith(canonicalRoot + File.separator)) {
                response.setStatusCode(403);
                response.setBody("Forbidden: Cannot delete files outside root directory".getBytes());
                response.addHeader("Content-Type", "text/plain");
                return response;
            }
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody("Internal Server Error: Path validation failed".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Check if file exists
        if (!fileToDelete.exists()) {
            response.setStatusCode(404);
            response.setBody("Not Found: File does not exist".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Check if it's a directory
        if (fileToDelete.isDirectory()) {
            // Optionally: recursively delete or return error
            File[] files = fileToDelete.listFiles();
            if (files != null && files.length > 0) {
                response.setStatusCode(409);
                response.setBody("Conflict: Directory is not empty".getBytes());
                response.addHeader("Content-Type", "text/plain");
                return response;
            }
        }
        
        // Attempt to delete the file
        boolean deleted;
        try {
            deleted = Files.deleteIfExists(fileToDelete.toPath());
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody(("Internal Server Error: Failed to delete file - " + e.getMessage()).getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        if (deleted) {
            response.setStatusCode(200);
            response.setBody(("Deleted: " + path).getBytes());
            response.addHeader("Content-Type", "text/plain");
        } else {
            response.setStatusCode(404);
            response.setBody("Not Found: File could not be deleted".getBytes());
            response.addHeader("Content-Type", "text/plain");
        }
        
        return response;
    }
}
