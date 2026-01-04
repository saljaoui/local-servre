package handlers;

import config.model.WebServerConfig.ServerBlock;
import handlers.model.Upload;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Import the fixed parser
import java.nio.file.StandardCopyOption;
import routing.model.Route;

public class UploadHandler {
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
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
        // byte[] rawBody = request.getBody()
        File uploaFile = request.getUploadedFile();
        // System.out.println("UploadHandler.handle() "+rawBody.toString());
        if (uploaFile == null || !uploaFile.exists()) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }

        long fileSize = uploaFile.length();

        // 5. Call Parser (Extract Clean Content)
 
        System.out.println("[CH] [UPLOAD] Calling MultipartParser...");

        // 6. Check Size (Server Limit)
        long maxBodySize = server.getClientMaxBodyBytes();
        if (fileSize > maxBodySize) {
            return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
        }

     
String filename =
        System.currentTimeMillis() + "_" +
        java.util.UUID.randomUUID().toString().substring(0, 8);
        // 7. Create destination file
        File destinationFile = new File(uploadDirectory, filename); 

        try {
            // 8. Move temp file to destination
            Files.move(
                    uploaFile.toPath(),
                    destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[UPLOAD] File moved from: " + uploaFile.getAbsolutePath());
            System.out.println("[UPLOAD] File moved to: " + destinationFile.getAbsolutePath());
            System.out.println("[UPLOAD] File size: " + destinationFile.length() + " bytes");

            // 9. Build Response
            response.setStatus(HttpStatus.OK);
            String msg = "File uploaded successfully: " + filename;
            response.setBody(msg.getBytes());
 
        } catch (IOException e) {
            e.printStackTrace();
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
