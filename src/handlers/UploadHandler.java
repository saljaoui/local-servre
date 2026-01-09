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
import util.MultipartParser;

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

        // 4. Get Body (multipart upload saved to temp file or raw body)
        File uploadFile = request.getUploadedFile();
        byte[] rawBody = request.getBody();
        boolean hasFile = uploadFile != null && uploadFile.exists();
        boolean hasRaw = rawBody != null && rawBody.length > 0;
        if (!hasFile && !hasRaw) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }

        long fileSize = hasFile ? uploadFile.length() : rawBody.length;

        // 6. Check Size (Server Limit)
        long maxBodySize = server.getClientMaxBodyBytes();
        if (fileSize > maxBodySize) {
            return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String filename= System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        // 7. Create destination file
        File destinationFile = new File(uploadDirectory, filename);

        try {
            if (hasFile) {
                byte[] extracted = extractMultipartIfNeeded(request, uploadFile);
                if (extracted != null) {
                    Files.write(destinationFile.toPath(), extracted);
                } else {
                    Files.move(
                            uploadFile.toPath(),
                            destinationFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                byte[] extracted = extractMultipartIfNeeded(request, rawBody);
                Files.write(destinationFile.toPath(), extracted != null ? extracted : rawBody);
            }

            if (hasFile) {
                System.out.println("[UPLOAD] File moved from: " + uploadFile.getAbsolutePath());
            }
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

    private byte[] extractMultipartIfNeeded(HttpRequest request, byte[] rawBody) {
        String ct = request.getHeader("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("multipart/form-data")) {
            return null;
        }
        return MultipartParser.extractFileContent(request);
    }

    private byte[] extractMultipartIfNeeded(HttpRequest request, File rawFile) throws IOException {
        String ct = request.getHeader("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("multipart/form-data")) {
            return null;
        }
        byte[] rawBody = Files.readAllBytes(rawFile.toPath());
        HttpRequest clone = new HttpRequest();
        clone.setHeaders(request.getHeaders());
        clone.setBody(rawBody);
        return MultipartParser.extractFileContent(clone);
    }
}
