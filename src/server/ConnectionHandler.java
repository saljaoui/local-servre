package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.SocketChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import routing.Router;
import session.SessionManager;
import util.SonicLogger;

public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private final ErrorHandler errorHandler;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private final Router router;
    private final ServerBlock server;

    //
    private ByteArrayOutputStream rawBytes;
    private boolean isFileUpload = false;
    private File tempFile;
    private FileOutputStream tempFileOutputStream;
    private long bytesWrittenToFile = 0;
    private String boundary;
    private String uploadFileName;
    private boolean headersParsed = false;
    private boolean isChunked = false;
    private int headerEndIndex = -1;
    private byte[] remainingHeaderData;
    private boolean requestComplete = false;
    private boolean headersReceived = false;
    private long expectedContentLength = 0;
    private long totalBytesRead = 0;

    private boolean skippedMultipartHeaders = false;
    private byte[] boundaryBytes;
    private boolean inFileContent = false;
    private boolean foundFileStart = false;
    private boolean fileComplete = false;

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
        this.router = new Router();
        this.errorHandler = new ErrorHandler();
    }

    public ServerBlock getServer() {
        return server;
    }

    public boolean read(ServerBlock server) throws IOException {
        int bytesRead = channel.read(readBuffer);

        if (bytesRead == -1) {
            this.close();
            return false;
        }

        totalBytesRead += bytesRead;

        // Check max body size
        if (totalBytesRead > server.getClientMaxBodyBytes()) {
            try {
                cleanupTempFile();
                httpResponse = errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
                ByteBuffer buffer = ByteBuffer.wrap(httpResponse.toString().getBytes());
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            } catch (IOException e) {
                httpResponse = errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
            }
            this.close();
            return false;
        }
        
        // Process the data
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();
        
        if (rawBytes == null) {
            rawBytes = new ByteArrayOutputStream();
        }
        rawBytes.write(data);
        
        // Parse headers to get Content-Length or Transfer-Encoding
        if (!headersParsed) {
            String temp = new String(rawBytes.toByteArray());
            int idx = temp.indexOf("\r\n\r\n");
            if (idx != -1) {
                headersParsed = true;
                headerEndIndex = idx;

                for (String line : temp.substring(0, idx).split("\r\n")) {
                    String lower = line.toLowerCase();
                    if (lower.startsWith("content-length:")) {
                        expectedContentLength = Long.parseLong(line.split(":")[1].trim());
                    } else if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {
                        isChunked = true;
                        expectedContentLength = 0;
                    }
                }

                if (isMulltipartForm()) {
                    isFileUpload = true;
                    boundary = extractBoundry(temp);
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    prepareFileUpload();

                    // Process any body data that came with the headers
                    byte[] bodyData = Arrays.copyOfRange(rawBytes.toByteArray(), idx + 4, rawBytes.size());
                    if (bodyData.length > 0 && !isChunked) {
                        processFileData(bodyData);
                    }
                }
            }
        } else if (isFileUpload && !fileComplete && !isChunked) {
            processFileData(data);
        }

        if (headersParsed && isChunked) {
            ChunkDecodeResult chunkResult = tryDecodeChunked(rawBytes.toByteArray(), headerEndIndex);
            if (!chunkResult.complete) {
                return false; // need more data
            }

            byte[] decodedBody = chunkResult.body;
            if (decodedBody.length > server.getClientMaxBodyBytes()) {
                cleanupTempFile();
                httpResponse = errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
                ByteBuffer buffer = ByteBuffer.wrap(httpResponse.toString().getBytes());
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                this.close();
                return false;
            }

            // Rebuild raw request with Content-Length and decoded body
            String headersPart = new String(rawBytes.toByteArray(), 0, headerEndIndex, StandardCharsets.ISO_8859_1);
            String normalizedHeaders = normalizeHeaders(headersPart, decodedBody.length);

            rawBytes = new ByteArrayOutputStream();
            rawBytes.write(normalizedHeaders.getBytes(StandardCharsets.ISO_8859_1));
            rawBytes.write(decodedBody);
            expectedContentLength = decodedBody.length;

            if (isFileUpload && !fileComplete) {
                processFileData(decodedBody);
            }

            requestComplete = isFileUpload ? fileComplete : true;
        } else if (headersParsed) {
            if (isFileUpload) {
                requestComplete = fileComplete || (expectedContentLength == 0 || bytesWrittenToFile >= expectedContentLength);
            } else {
                // For regular requests, check if we have the complete body
                requestComplete = expectedContentLength == 0 || rawBytes.size() >= expectedContentLength;
            }
        }

        return requestComplete;
    }

    public File getUploadedFile() {
        if (!isFileUpload || tempFile == null) {
            return null;
        }

        // Close the output stream if it's still open
        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
                tempFileOutputStream = null;
            }
        } catch (IOException e) {
            System.err.println("Error closing temp file output stream: " + e.getMessage());
        }

        return tempFile;
    }

    // Add a method to get the uploaded filename
    // public String getUploadFileName() {
    //     if (uploadFileName == null && isFileUpload) {
    //         // Extract filename from the raw request data
    //         String temp = new String(rawBytes.toByteArray());
    //         uploadFileName = extractFilename(temp, boundary);
    //     }
    //     return uploadFileName;
    // }

    // private String extractFilename(String headerString, String boundary) {
    //     // Find the first part of the multipart data
    //     int firstBoundary = headerString.indexOf("--" + boundary);
    //     if (firstBoundary == -1)
    //         return null;

    //     int secondBoundary = headerString.indexOf("--" + boundary, firstBoundary + 2);
    //     if (secondBoundary == -1)
    //         return null;

    //     String firstPart = headerString.substring(firstBoundary, secondBoundary);

    //     // Find Content-Disposition header
    //     int dispositionIndex = firstPart.indexOf("Content-Disposition:");
    //     if (dispositionIndex == -1)
    //         return null;

    //     // Find filename parameter
    //     int filenameIndex = firstPart.indexOf("filename=\"", dispositionIndex);
    //     if (filenameIndex == -1)
    //         return null;

    //     int filenameStart = filenameIndex + 10;
    //     int filenameEnd = firstPart.indexOf("\"", filenameStart);
    //     if (filenameEnd == -1)
    //         return null;

    //     String filename = firstPart.substring(filenameStart, filenameEnd);

    //     // Extract just the filename without path
    //     int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
    //     if (lastSlash != -1) {
    //         filename = filename.substring(lastSlash + 1);
    //     }

    //     return filename.isEmpty() ? null : filename;
    // }

    public void cleanupTempFile() {
        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
                tempFileOutputStream = null;
            }

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                System.out.println("Deleted temp file: " + tempFile.getPath());
            }
        } catch (IOException e) {
            System.err.println("Error cleaning up temp file: " + e.getMessage());
        }
    }

    private void processFileData(byte[] bodyData) throws IOException {
        if (tempFileOutputStream == null || fileComplete) {
            return;
        }
        
        // If we haven't found the start of the file content yet
        if (!foundFileStart) {
            // System.out.println("Searching for start of file content...");
            String dataStr = new String(bodyData);
            int contentStart = dataStr.indexOf("\r\n\r\n");
            
            if (contentStart != -1) {
                // Found the end of headers, start of file content
                foundFileStart = true;
                inFileContent = true;
                
                // Write only the content after the headers
                byte[] fileContent = Arrays.copyOfRange(bodyData, contentStart + 4, bodyData.length);
                
                // Check if this content already contains the end boundary
                int boundaryPos = findBoundary(fileContent, boundaryBytes);
                if (boundaryPos != -1) {
                    // Write content up to the boundary
                    tempFileOutputStream.write(fileContent, 0, boundaryPos);
                    bytesWrittenToFile += boundaryPos;
                    inFileContent = false;
                    fileComplete = true;
                    System.out.println("File upload complete: " + bytesWrittenToFile + " bytes");
                } else {
                    // Write all content
                    tempFileOutputStream.write(fileContent);
                    bytesWrittenToFile += fileContent.length;
                }
            }
            // If we haven't found the start yet, don't write anything
        } else if (inFileContent) {
            // We're in the file content, check for the end boundary
            int boundaryPos = findBoundary(bodyData, boundaryBytes);
            
            if (boundaryPos != -1) {
                // Found the end boundary, write content up to it
                tempFileOutputStream.write(bodyData, 0, boundaryPos);
                bytesWrittenToFile += boundaryPos;
                inFileContent = false;
                fileComplete = true;
                System.out.println("File upload complete: " + bytesWrittenToFile + " bytes");
            } else {
                // No boundary yet, write all content
                tempFileOutputStream.write(bodyData);
                bytesWrittenToFile += bodyData.length;
            }
        }
        
        // Log progress for large uploads
        if (bytesWrittenToFile % (1024 * 1024) == 0) { // Every 1MB
            System.out.println("Received " + (bytesWrittenToFile / 1024 / 1024) + "MB of " +
                    (expectedContentLength / 1024 / 1024) + "MB");
        }
    }
    
    // Helper method to find boundary in byte array
    private int findBoundary(byte[] data, byte[] boundary) {
        if (data == null || boundary == null || data.length < boundary.length) {
            return -1;
        }
        
        for (int i = 0; i <= data.length - boundary.length; i++) {
            boolean match = true;
            for (int j = 0; j < boundary.length; j++) {
                if (data[i + j] != boundary[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private void prepareFileUpload() throws IOException {
        tempFile = File.createTempFile("uploads", ".tmp");
        tempFileOutputStream = new FileOutputStream(tempFile);
        
        // Create metadata file
        File metadataFile = new File(tempFile.getParent(), tempFile.getName() + ".meta");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            // writer.write("original_filename=" + getUploadFileName() + "\n");
            writer.write("upload_time=" + System.currentTimeMillis() + "\n");
        }

        System.out.println("Created temp file: " + tempFile.getPath());
        System.out.println("Created metadata file: " + metadataFile.getPath());
    }

    private String extractBoundry(String headeString) {
        String[] lines = headeString.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-type: multipart/form-data")) {
                int boundaryIndex = line.indexOf("boundary=");
                if (boundaryIndex != -1) {
                    return line.substring(boundaryIndex + 9);
                }
            }
        }
        return null;
    }

    // Add this method to ConnectionHandler class
    public boolean isFileUpload() {
        return isFileUpload;
    }

    private boolean isMulltipartForm() {
        if (rawBytes == null) {
            return false;
        }
        String tmp = new String(rawBytes.toByteArray());
        return tmp.toLowerCase().contains("content-type: multipart/form-data");
    }

    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(rawBytes.toByteArray());
            httpRequest.setConnectionHandler(this);
            SessionManager.getInstance().attachSession(httpRequest);
            httpResponse = router.routeRequest(httpRequest, server);
            SessionManager.getInstance().appendSessionCookie(httpRequest, httpResponse);
        } catch (Exception ex) {
            logger.error("Error processing request", ex);
            httpResponse = errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        prepareResponseBuffer();
    }

    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }

        channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            resetForNextRequest();
            return true;
        }
        return false;
    }

    public void close() throws IOException {
        channel.close();
    }

    private void prepareResponseBuffer() {
        byte[] body = httpResponse.getBody() == null ? new byte[0] : httpResponse.getBody();

        // Get status message
        String reason = httpResponse.getStatusMessage();
        if (reason == null || reason.isEmpty()) {
            HttpStatus statusEnum = resolveStatus(httpResponse.getStatusCode());
            reason = statusEnum != null ? statusEnum.message : "OK";
        }

        // Set default headers
        httpResponse.getHeaders().putIfAbsent(
                "Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        httpResponse.getHeaders().putIfAbsent("Connection", "close");
        httpResponse.getHeaders().putIfAbsent("Content-Length", String.valueOf(body.length));

        // Build response
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(httpResponse.getStatusCode()).append(" ").append(reason).append("\r\n");
        httpResponse.getHeaders().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes();
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    private void resetForNextRequest() {
        headersReceived = false;
        expectedContentLength = 0;
        totalBytesRead = 0;
        headersParsed = false;
        isChunked = false;
        isFileUpload = false;
        bytesWrittenToFile = 0;
        requestComplete = false;
        boundary = null;
        headerEndIndex = -1;
        uploadFileName = null;
        foundFileStart = false;
        inFileContent = false;
        fileComplete = false;

        if (rawBytes != null) {
            rawBytes = new ByteArrayOutputStream();
        }

        httpRequest = null;
        httpResponse = null;
        writeBuffer = null;
    }

    private HttpStatus resolveStatus(int code) {
        for (HttpStatus status : HttpStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    private String normalizeHeaders(String headerSection, long contentLength) {
        StringBuilder sb = new StringBuilder();
        String[] lines = headerSection.split("\r\n");
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.startsWith("transfer-encoding:")) continue;
            if (lower.startsWith("content-length:")) continue;
            sb.append(line).append("\r\n");
        }
        sb.append("Content-Length: ").append(contentLength).append("\r\n\r\n");
        return sb.toString();
    }

    private ChunkDecodeResult tryDecodeChunked(byte[] data, int headerEnd) {
        int idx = headerEnd + 4;
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        while (idx < data.length) {
            int lineEnd = indexOfCrlf(data, idx);
            if (lineEnd == -1) {
                return ChunkDecodeResult.incomplete();
            }

            String sizeLine = new String(data, idx, lineEnd - idx, StandardCharsets.ISO_8859_1).trim();
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException e) {
                return ChunkDecodeResult.incomplete();
            }

            idx = lineEnd + 2; // past CRLF
            if (chunkSize == 0) {
                if (idx + 2 <= data.length) {
                    return ChunkDecodeResult.complete(body.toByteArray());
                }
                return ChunkDecodeResult.incomplete();
            }

            if (idx + chunkSize > data.length) {
                return ChunkDecodeResult.incomplete();
            }

            body.write(data, idx, chunkSize);
            idx += chunkSize;

            if (idx + 2 > data.length) {
                return ChunkDecodeResult.incomplete();
            }
            idx += 2; // skip trailing CRLF
        }

        return ChunkDecodeResult.incomplete();
    }

    private int indexOfCrlf(byte[] data, int start) {
        for (int i = start; i < data.length - 1; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static class ChunkDecodeResult {
        final boolean complete;
        final byte[] body;

        private ChunkDecodeResult(boolean complete, byte[] body) {
            this.complete = complete;
            this.body = body;
        }

        static ChunkDecodeResult complete(byte[] body) {
            return new ChunkDecodeResult(true, body);
        }

        static ChunkDecodeResult incomplete() {
            return new ChunkDecodeResult(false, null);
        }
    }
}
