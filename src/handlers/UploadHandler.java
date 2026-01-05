package handlers;

import config.model.WebServerConfig.ServerBlock;
import handlers.model.Upload;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import routing.model.Route;
import util.SonicLogger;

public class UploadHandler {
    private static final SonicLogger logger = SonicLogger.getLogger(UploadHandler.class);
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        Upload upload = route.getUpload();
        if (upload == null || !upload.isEnabled()) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }

        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) {
            return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
        }

        String uploadDir = upload.getDir();
        if (uploadDir == null || uploadDir.isEmpty()) {
            uploadDir = "uploads";
        }
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        File uploadedFile = request.getUploadedFile();
        byte[] rawBody = request.getBody();
        boolean hasFile = uploadedFile != null && uploadedFile.exists();
        boolean hasRaw = rawBody != null && rawBody.length > 0;
        
        if (!hasFile && !hasRaw) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }

        long fileSize = hasFile ? uploadedFile.length() : rawBody.length;
        long maxBodySize = server.getClientMaxBodyBytes();
        
        if (fileSize > maxBodySize) {
            return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String filename = System.currentTimeMillis() + "_" + 
                         java.util.UUID.randomUUID().toString().substring(0, 8);
        
        File destinationFile = new File(uploadDirectory, filename);

        try {
            if (hasFile) {
                Files.move(
                    uploadedFile.toPath(),
                    destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(destinationFile.toPath(), rawBody);
            }

            logger.info("File uploaded: " + filename + " (" + fileSize + " bytes)");

            response.setStatus(HttpStatus.OK);
            String msg = "File uploaded successfully: " + filename;
            response.setBody(msg.getBytes());
            response.addHeader("Content-Type", "text/plain");

        } catch (IOException e) {
            logger.error("Upload failed for " + filename, e);
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
