package handlers;

import config.model.WebServerConfig.ServerBlock;
import config.model.WebServerConfig.Upload;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Import the fixed parser
import java.nio.file.StandardCopyOption;
import routing.model.Route;
import util.MimeTypes;
import util.MultipartParser;

public class UploadHandler {
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();

        System.out.println("=".repeat(60)); // Separator in console

        // 1. Check if upload is enabled
        Upload upload = route.getUpload();
        if (upload == null || !upload.isEnabled()) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }

        // 2. Check Method
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) {
            return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
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
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }

        // 5. Call Parser (Extract Clean Content)
        System.out.println("[CH] [UPLOAD] Calling MultipartParser...");
        byte[] cleanContent = MultipartParser.extractFileContent(request);
        if (cleanContent == null) {
            System.err.println("[CRITICAL] [UPLOAD] MultipartParser returned NULL! Writing 0 bytes.");
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        System.out.println("[CH] [UPLOAD] Parser returned clean content. Size: " + cleanContent.length);

        // 6. Check Size (Server Limit)
        long maxBodySize = server.getClientMaxBodyBytes();
        if (cleanContent.length > maxBodySize) {
            return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
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
            response.setStatusCode(HttpStatus.OK.code);
            response.setStatusMessage(HttpStatus.OK.message);
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
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
