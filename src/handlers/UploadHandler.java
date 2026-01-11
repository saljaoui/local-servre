package handlers;

import config.model.WebServerConfig.ServerBlock;
import handlers.model.Upload;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import routing.model.Route;

public class UploadHandler {

    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        Upload upload = route.getUpload();
        if (upload == null || !upload.isEnabled()) {
            return errorHandler.handle(server, HttpStatus.FORBIDDEN);
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
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

        File uploadFile = request.getUploadedFile();
        byte[] rawBody = request.getBody();
        boolean hasFile = uploadFile != null && uploadFile.exists();
        boolean hasRaw = rawBody != null && rawBody.length > 0;
        
        if (!hasFile && !hasRaw) {
            return errorHandler.handle(server, HttpStatus.BAD_REQUEST);
        }

        long fileSize = hasFile ? uploadFile.length() : rawBody.length;
        if (fileSize > server.getClientMaxBodyBytes()) {
            return errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String extension = detectExtension(uploadFile, rawBody);
        String filename = System.currentTimeMillis() + "_" + 
                         java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
        File destinationFile = new File(uploadDirectory, filename);

        try {
            if (hasFile) {
                Files.move(uploadFile.toPath(), destinationFile.toPath(), 
                          StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(destinationFile.toPath(), rawBody);
            }

            response.setStatus(HttpStatus.OK);
            response.setBody(("File uploaded successfully: " + filename).getBytes());

        } catch (IOException e) {
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private String detectExtension(File file, byte[] data) {
        byte[] magic = new byte[12];
        
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(magic);
            } catch (IOException e) {
                return "";
            }
        } else if (data != null && data.length >= 12) {
            System.arraycopy(data, 0, magic, 0, 12);
        } else {
            return "";
        }

        // MP4: ftyp at bytes 4-7
        if (magic.length >= 8 && new String(magic, 4, 4).startsWith("ftyp")) {
            return ".mp4";
        }
        // PNG: 89 50 4E 47
        if (magic[0] == (byte)0x89 && magic[1] == 0x50 && magic[2] == 0x4E && magic[3] == 0x47) {
            return ".png";
        }
        // JPEG: FF D8 FF
        if (magic[0] == (byte)0xFF && magic[1] == (byte)0xD8 && magic[2] == (byte)0xFF) {
            return ".jpg";
        }
        // GIF: 47 49 46 38
        if (magic[0] == 0x47 && magic[1] == 0x49 && magic[2] == 0x46 && magic[3] == 0x38) {
            return ".gif";
        }
        // PDF: 25 50 44 46
        if (magic[0] == 0x25 && magic[1] == 0x50 && magic[2] == 0x44 && magic[3] == 0x46) {
            return ".pdf";
        }
        // ZIP: 50 4B 03 04
        if (magic[0] == 0x50 && magic[1] == 0x4B && magic[2] == 0x03 && magic[3] == 0x04) {
            return ".zip";
        }
        
        return "";
    }
}