package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import config.model.WebServerConfig.ServerBlock;
import config.model.WebServerConfig.Upload;
import routing.model.Route;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class UploadHandler {
    
    /**
     * Handle file upload requests (POST).
     * Saves uploaded files to the configured directory.
     */
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        // Check if upload is enabled for this route
        Upload upload = route.getUpload();
        // System.err.println("[DEBUG] Upload config: " + request.get);
        if (upload == null || !upload.isEnabled()) {
            response.setStatusCode(403);
            response.setBody("Forbidden: Upload not enabled for this route".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Check request method
        String method = request.getMethod();
         if (!"POST".equalsIgnoreCase(method)) {
            response.setStatusCode(405);
            response.setBody("Method Not Allowed: Only POST method is supported for uploads".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Get upload configuration
        String uploadDir = upload.getDir();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "uploads";
        }
        
        String fileField = upload.getFileField();
        System.err.println("[DEBUG] Original filename: " + fileField);
        if (fileField == null || fileField.isEmpty()) {
            fileField = "file";
        }
        
        // Ensure upload directory exists
        File uploadDirectory = new File(uploadDir);
        System.err.println("[DEBUG] Upload directory: " + uploadDirectory.getAbsolutePath());
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        byte[] body = request.getBody();
        if (body == null || body.length == 0) {
            response.setStatusCode(400);
            response.setBody("Bad Request: No file content provided".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        // Check file size against server's max body size limit
        long maxBodySize = server.getClientMaxBodyBytes();
        if (body.length > maxBodySize) {
            response.setStatusCode(413);
            response.setBody("Payload Too Large: File exceeds maximum allowed size".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
 
        return response;
    }
    
  
}
