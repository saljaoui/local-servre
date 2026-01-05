package handlers;

import config.model.WebServerConfig.ServerBlock;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import routing.model.Route;
import util.SonicLogger;
 
public class DeleteHandler {
    private static final SonicLogger logger = SonicLogger.getLogger(DeleteHandler.class);
    private final ErrorHandler errorHandler = new ErrorHandler();
    
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        String path = request.getPath();
        if (path == null || path.isEmpty() || path.equals("/")) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }
        
        // Get root directory
        String root = route.getRoot();
        if (root == null || root.isEmpty()) {
            root = server.getRoot();
        }
        if (root == null || root.isEmpty()) {
            root = "./www";
        }
        
        // Extract relative path by removing route prefix
        String relativePath = path;
        if (path.startsWith(route.getPath())) {
            relativePath = path.substring(route.getPath().length());
        }
        
        // Remove leading slash
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        // Build file path
        File fileToDelete = new File(root, relativePath);
        
        logger.debug("DELETE request - Path: " + path + ", Root: " + root + 
                    ", Relative: " + relativePath + ", File: " + fileToDelete.getAbsolutePath());
        
        // Security check: ensure the file is within the allowed directory
        try {
            String canonicalRoot = new File(root).getCanonicalPath();
            String canonicalFile = fileToDelete.getCanonicalPath();
            
            if (!canonicalFile.startsWith(canonicalRoot)) {
                logger.warn("Path traversal blocked: " + path);
                return errorHandler.handle(server, HttpStatus.FORBIDDEN);
            }
        } catch (IOException e) {
            logger.error("Error resolving canonical path", e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Check if file exists
        if (!fileToDelete.exists()) {
            logger.debug("File not found: " + fileToDelete.getAbsolutePath());
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }
        
        // Check if it's a directory
        if (fileToDelete.isDirectory()) {
            File[] files = fileToDelete.listFiles();
            if (files != null && files.length > 0) {
                return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
            }
        }
        
        // Attempt to delete the file
        try {
            boolean deleted = Files.deleteIfExists(fileToDelete.toPath());
            
            if (deleted) {
                logger.info("File deleted: " + fileToDelete.getAbsolutePath());
                response.setStatus(HttpStatus.OK);
                response.setBody(("Deleted: " + path).getBytes());
                response.addHeader("Content-Type", "text/plain");
            } else {
                return errorHandler.handle(server, HttpStatus.NOT_FOUND);
            }
        } catch (IOException e) {
            logger.error("Error deleting file: " + fileToDelete.getAbsolutePath(), e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return response;
    }
}
