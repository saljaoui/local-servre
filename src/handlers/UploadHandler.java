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
        if (fileField == null || fileField.isEmpty()) {
            fileField = "file";
        }
        
        // Ensure upload directory exists
        File uploadDirectory = new File(uploadDir);
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
        
        // Generate a unique filename to prevent overwrites
        String originalFilename = extractFilename(request, fileField);
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "uploaded_file";
        }
        
        // Sanitize filename to prevent directory traversal
        originalFilename = new File(originalFilename).getName();
        
        String uniqueFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
        File uploadedFile = new File(uploadDirectory, uniqueFilename);
        
        try {
            // Write the file
            Files.copy(
                new java.io.ByteArrayInputStream(body),
                uploadedFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
            
            response.setStatusCode(200);
            String successMessage = "File uploaded successfully: " + originalFilename + 
                                    " (saved as: " + uniqueFilename + ")";
            response.setBody(successMessage.getBytes());
            response.addHeader("Content-Type", "text/plain");
            response.addHeader("Content-Length", String.valueOf(successMessage.length()));
            
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody(("Internal Server Error: Failed to save file - " + e.getMessage()).getBytes());
            response.addHeader("Content-Type", "text/plain");
        }
        
        return response;
    }
    
    /**
     * Extract filename from request headers or body.
     * This is a simple implementation - for multipart uploads, 
     * a full parser would be needed.
     */
    private String extractFilename(HttpRequest request, String fieldName) {
        // Try to get filename from Content-Disposition header
        String contentDisposition = request.getHeader("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename")) {
            int filenameIndex = contentDisposition.indexOf("filename");
            int start = contentDisposition.indexOf("\"", filenameIndex);
            int end = contentDisposition.indexOf("\"", start + 1);
            if (start >= 0 && end > start) {
                return contentDisposition.substring(start + 1, end);
            }
        }
        
        // Return default name
        return "uploaded_file";
    }
}
