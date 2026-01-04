package handlers;

import config.model.WebServerConfig.ServerBlock;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import routing.model.Route;
 
public class DeleteHandler {

    
    private final ErrorHandler errorHandler = new ErrorHandler();
    
    /**
     * Handle DELETE requests.
     * Deletes the specified file or resource.
     */
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        String path = request.getPath();
        if (path == null || path.isEmpty() || path.equals("/")) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
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
                return errorHandler.handle(server, HttpStatus.FORBIDDEN);
            }
        } catch (IOException e) {
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Check if file exists
        if (!fileToDelete.exists()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        
        // Check if it's a directory
        if (fileToDelete.isDirectory()) {
            // Optionally: recursively delete or return error
            File[] files = fileToDelete.listFiles();
            if (files != null && files.length > 0) {
                return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
            }
        }
        
        // Attempt to delete the file
        boolean deleted;
        try {
            deleted = Files.deleteIfExists(fileToDelete.toPath());
        } catch (IOException e) {
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        if (deleted) {
            response.setStatus(HttpStatus.OK);
            response.setBody(("Deleted: " + path).getBytes());
            response.addHeader("Content-Type", "text/plain");
        } else {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        
        return response;
    }
}
