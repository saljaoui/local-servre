package handlers;

import config.model.WebServerConfig.ServerBlock;
import config.model.WebServerConfig.Upload;
import http.model.HttpRequest;
import http.model.HttpResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Import the fixed parser
import java.nio.file.StandardCopyOption;
import routing.model.Route;
import util.MimeTypes;
import util.MultipartParser;

public class UploadHandler {

    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();

        System.out.println("=".repeat(60)); // Separator in console

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

        // 3. Get Upload Directory
        String uploadDir = upload.getDir();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "uploads";
        }
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        // 4. Get Body
        byte[] rawBody = request.getBody();

        if (rawBody == null || rawBody.length == 0) {
            response.setStatusCode(400);
            response.setBody("Bad Request: No file content provided".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        // 5. Call Parser (Extract Clean Content)
        System.out.println("[CH] [UPLOAD] Calling MultipartParser...");
        byte[] cleanContent = MultipartParser.extractFileContent(request);
        if (cleanContent == null) {
            System.err.println("[CRITICAL] [UPLOAD] MultipartParser returned NULL! Writing 0 bytes.");
            response.setStatusCode(500);
            response.setBody("Internal Error: Failed to parse file content".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        System.out.println("[CH] [UPLOAD] Parser returned clean content. Size: " + cleanContent.length);

        // 6. Check Size (Server Limit)
        long maxBodySize = server.getClientMaxBodyBytes();
        if (cleanContent.length > maxBodySize) {
            response.setStatusCode(413);
            response.setBody("Payload Too Large: File exceeds maximum allowed size".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        String fileField = upload.getFileField();
        if (fileField == null || fileField.isEmpty()) {
            fileField = "file";
        }

        String originalName = MultipartParser.extractFilename(request);

        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String filename = System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8) + extension;

        File uploadedFile = new File(uploadDirectory, filename);

        try {
            // 8. Write File
            Files.copy(
                    new java.io.ByteArrayInputStream(cleanContent),
                    uploadedFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            // DEBUGGING: Verify what is on disk immediately
            long diskSize = uploadedFile.length();
            System.out.println("[CH] [UPLOAD] File Saved to: " + uploadedFile.getAbsolutePath());
            System.out.println("[CH] [UPLOAD] Size on Disk: " + diskSize + " bytes");
            System.out.println("[CH] [UPLOAD] Input RAM Size: " + cleanContent.length + " bytes");

            // CHECK: Did we save 4 bytes?
            if (diskSize <= 10) {
                System.err.println("[CRITICAL] [UPLOAD] File looks CORRUPT! Only " + diskSize + " bytes saved.");
            }

            // 9. Build Response
            response.setStatusCode(200);
            String msg = "File saved: " + filename;
            response.setBody(msg.getBytes());

            // 10. SET CORRECT CONTENT-TYPE
            // Detect type from the clean binary data
            String mimeType = MimeTypes.getMimeType(uploadedFile.getName());
            System.out.println("[CH] [UPLOAD] Detected MIME Type: " + mimeType);
            response.addHeader("Content-Type", mimeType);

            System.out.println("=".repeat(60));

        } catch (IOException e) {
            e.printStackTrace();
            response.setStatusCode(500);
            // response.setBody("Internal Error: " + e.getMessage().getBytes());
            response.addHeader("Content-Type", "text/plain");
        }

        return response;
    }
}