package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import config.model.WebServerConfig.ServerBlock;
import config.model.WebServerConfig.Upload;
import routing.model.Route;
import util.MultipartParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UploadHandler {
 
 
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();

        // 1. Check if upload is enabled
        Upload upload = route.getUpload();
        if (upload == null || !upload.isEnabled()) {
            response.setStatusCode(403);
            response.setBody("Forbidden: Upload not enabled".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        // 2. Check Method
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) {
            response.setStatusCode(405);
            response.setBody("Method Not Allowed".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        // 3. Get Directory
        String uploadDir = upload.getDir();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "uploads"; // Default folder
        }

        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        // 4. Get Body (Raw Data)
        byte[] body = request.getBody();
        
        System.out.println("[CH] [UPLOAD] Final Body Size (Raw): " + body.length);

        if (body == null || body.length == 0) {
            response.setStatusCode(400);
            response.setBody("Bad Request: No file content provided".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        // 5. Check Size
        long maxBodySize = server.getClientMaxBodyBytes();
        if (body.length > maxBodySize) {
            response.setStatusCode(413);
            response.setBody("Payload Too Large".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        // =================================================================
        // THE MISSING CALL: Extract Pure File Content
        // =================================================================
        System.out.println("[CH] [UPLOAD] Calling MultipartParser to strip headers...");
        byte[] cleanContent = MultipartParser.extractFileContent(request);
        
        if (cleanContent == null) {
            cleanContent = body; // Fallback if not multipart
        }

        System.out.println("[CH] [UPLOAD] Clean Content Size: " + cleanContent.length);

        // 6. Generate Filename
        String filename = System.currentTimeMillis() + "_" + 
                           java.util.UUID.randomUUID().toString().substring(0, 8) + ".uploaded";
        
        File uploadedFile = new File(uploadDirectory, filename);

        try {
            // 7. Write file (Using CLEAN content)
            Files.copy(
                    new java.io.ByteArrayInputStream(cleanContent),
                    uploadedFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );

            // =================================================================
            // DEBUG 7: Verify saved file size
            // =================================================================
            long savedSize = uploadedFile.length();
            System.out.println("[CH] [UPLOAD] File Saved to: " + uploadedFile.getAbsolutePath());
            System.out.println("[CH] [UPLOAD] Saved File Size on Disk: " + savedSize + " bytes");

            if (savedSize != cleanContent.length) {
                System.err.println("[CH] [UPLOAD] WARNING: Sizes differ! Disk: " + savedSize + " vs RAM: " + cleanContent.length);
            }

            response.setStatusCode(200);
            String msg = "File saved: " + filename;
            response.setBody(msg.getBytes());
            response.addHeader("Content-Type", "text/plain");

        } catch (IOException e) {
            e.printStackTrace();
            response.setStatusCode(500);
            // response.setBody("Internal Error: " + e.getMessage().getBytes());
            response.addHeader("Content-Type", "text/plain");
        }

        return response;
    }

}
