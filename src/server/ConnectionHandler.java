package server;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import routing.Router;
import session.SessionManager;
import util.SonicLogger;

/**
 * ConnectionHandler processes HTTP requests in stages: 1. Read and validate
 * headers (max 16KB) 2. Validate request method and Content-Length 3. Read body
 * (memory or file based on size) 4. Parse and dispatch request 5. Send response
 */
public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);
    private static final int MAX_HEADER_SIZE = 16384; // 16KB
    private static final int READ_BUFFER_SIZE = 8192;
    private static final int MEMORY_THRESHOLD = 1024 * 1024; // 1MB

    // Network
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    private ByteBuffer writeBuffer;

    // Configuration
    private final ServerBlock server;
    private final Router router;
    private final ErrorHandler errorHandler;

    // Request Processing State
    private ProcessingState state = ProcessingState.READING_HEADERS;
    private ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream bodyBuffer = null;
    private FileOutputStream bodyFileStream = null;
    private File tempBodyFile = null;

    // Parsed Request Info
    private int headerEndIndex = -1;
    private long contentLength = 0;
    private long totalBytesRead = 0;
    private long bodyBytesRead = 0;
    private boolean isChunked = false;
    private boolean isMultipart = false;
    private String boundary = null;
    private String requestMethod = null;

    // Response
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    private enum ProcessingState {
        READING_HEADERS,
        READING_BODY_TO_MEMORY,
        READING_BODY_TO_FILE,
        REQUEST_COMPLETE,
        ERROR
    }

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
        this.router = new Router();
        this.errorHandler = new ErrorHandler();
    }

    public ServerBlock getServer() {
        return server;
    }

    /**
     * STEP 1: Read data from socket and process based on current state
     */
    public boolean read(ServerBlock server) throws IOException {
        int bytesRead = channel.read(readBuffer);

        // Connection closed by client
        if (bytesRead == -1) {
            cleanup();
            this.close();
            return false;
        }
        // System.out.println("ConnectionHandler.read() **  **  "+bytesRead+ "  *   *  "+ totalBytesRead);
        totalBytesRead += bytesRead;

        // Check total size limit early
        if (totalBytesRead > server.getClientMaxBodyBytes()) {
            handleError(HttpStatus.PAYLOAD_TOO_LARGE);
            return false;
        }

        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();

        // Process data based on current state
        // System.err.println(state+"  *   *  * ");
        try {
            return switch (state) {
                case READING_HEADERS ->
                    processHeaders(data, server);
                case READING_BODY_TO_MEMORY ->
                    processBodyToMemory(data, server);
                case READING_BODY_TO_FILE ->
                    processBodyToFile(data, server);
                case REQUEST_COMPLETE ->
                    true;
                case ERROR ->
                    false;
            };
        } catch (Exception e) {
            logger.error("Error processing request", e);
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

    }

    /**
     * STEP 2: Process headers (validate size, find header end)
     */
    private boolean processHeaders(byte[] data, ServerBlock server) throws IOException {
        headerBuffer.write(data);

        // Check header size limit (16KB)
        if (headerBuffer.size() > MAX_HEADER_SIZE) {
            handleError(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
            return false;
        }

        // Find header end marker in bytes
        byte[] headerData = headerBuffer.toByteArray();
        int headerEnd = findHeaderEnd(headerData);

        if (headerEnd == -1) {
            // Headers not complete yet, keep reading
            return false;
        }

        // STEP 3: Parse and validate headers
        headerEndIndex = headerEnd;
        return validateAndProcessHeaders(headerData, headerEnd, server);
    }

    /**
     * STEP 3: Validate headers (method, Content-Length, Transfer-Encoding)
     */
    private boolean validateAndProcessHeaders(byte[] headerData, int headerEnd, ServerBlock server)
            throws IOException {
        // Convert only headers to string
        String headersString = new String(headerData, 0, headerEnd, StandardCharsets.ISO_8859_1);
        String[] lines = headersString.split("\r\n");

        if (lines.length == 0) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        // Parse request line
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        requestMethod = requestLine[0].trim().toUpperCase();

        // Parse headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx <= 0) {
                continue;
            }
            // header 
            String name = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();
            // System.err.println("*-  "+ name+ " *  "+ value );
            switch (name) {
                case "content-length" -> {
                    try {
                        contentLength = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        handleError(HttpStatus.BAD_REQUEST);
                        return false;
                    }
                }
                case "transfer-encoding" -> {
                    if (value.toLowerCase().contains("chunked")) {
                        isChunked = true;
                    }
                }
                case "content-type" -> {
                    if (value.toLowerCase().contains("multipart/form-data")) {
                        isMultipart = true;
                        boundary = extractBoundary(value);
                    }
                }
            }
        }

        // STEP 4: Validate request requirements
        if ("POST".equals(requestMethod)) {
            // POST must have Content-Length or Transfer-Encoding: chunked
            if (contentLength == 0 && !isChunked) {
                handleError(HttpStatus.LENGTH_REQUIRED);
                return false;
            }
        }

        // Check if Content-Length exceeds limit
        if (contentLength > server.getClientMaxBodyBytes()) {
            handleError(HttpStatus.PAYLOAD_TOO_LARGE);
            return false;
        }

        // Handle chunked encoding
        if (isChunked) {
            handleError(HttpStatus.NOT_IMPLEMENTED); // For now, reject chunked
            return false;
        }

        // STEP 5: Check if there's body data after headers
        int bodyStart = headerEnd + 4; // Skip \r\n\r\n
        // System.out.println("ConnectionHandler.validateAndProcessHeaders()"+bodyStart);
        byte[] initialBodyData = null;
        if (bodyStart < headerData.length) {
            int initialBodyLength = headerData.length - bodyStart;
            initialBodyData = new byte[initialBodyLength];
            System.arraycopy(headerData, bodyStart, initialBodyData, 0, initialBodyLength);
            bodyBytesRead = initialBodyLength;
        }

        // STEP 6: Decide where to store body (memory vs file)
        if (contentLength == 0) {
            // No body expected
            state = ProcessingState.REQUEST_COMPLETE;
            return prepareRequest(headerData, headerEnd, new byte[0]);
        } else if (contentLength <= MEMORY_THRESHOLD) {
            // Small body - store in memory
            state = ProcessingState.READING_BODY_TO_MEMORY;
            bodyBuffer = new ByteArrayOutputStream((int) contentLength);
            if (initialBodyData != null) {
                bodyBuffer.write(initialBodyData);
            }
        } else {
            // Large body - store in file
            state = ProcessingState.READING_BODY_TO_FILE;
            tempBodyFile = createTempFile();
            bodyFileStream = new FileOutputStream(tempBodyFile);
            if (initialBodyData != null) {
                bodyFileStream.write(initialBodyData);
            }
        }

        // Check if body is already complete
        if (bodyBytesRead >= contentLength) {
            return finalizeBody(headerData, headerEnd);
        }

        return false; // Need more data
    }

    /**
     * STEP 7: Read body into memory
     */
    private boolean processBodyToMemory(byte[] data, ServerBlock server) throws IOException {
        long remaining = contentLength - bodyBytesRead;
        int toWrite = (int) Math.min(data.length, remaining);

        bodyBuffer.write(data, 0, toWrite);
        bodyBytesRead += toWrite;

        if (bodyBytesRead >= contentLength) {
            return finalizeBody(headerBuffer.toByteArray(), headerEndIndex);
        }

        return false;
    }

    /**
     * STEP 8: Read body into file
     */
    private boolean processBodyToFile(byte[] data, ServerBlock server) throws IOException {
        long remaining = contentLength - bodyBytesRead;
        int toWrite = (int) Math.min(data.length, remaining);

        bodyFileStream.write(data, 0, toWrite);
        bodyBytesRead += toWrite;

        if (bodyBytesRead >= contentLength) {
            bodyFileStream.close();
            return finalizeBody(headerBuffer.toByteArray(), headerEndIndex);
        }

        return false;
    }

    /**
     * STEP 9: Finalize body and prepare request
     */
    private boolean finalizeBody(byte[] headerData, int headerEnd) throws IOException {
        byte[] body;

        if (bodyBuffer != null) {
            body = bodyBuffer.toByteArray();
        } else if (tempBodyFile != null && tempBodyFile.exists()) {
            // For file uploads, we'll pass empty body but fix Content-Length
            body = new byte[0];
        } else {
            body = new byte[0];
        }

        state = ProcessingState.REQUEST_COMPLETE;
        return prepareRequest(headerData, headerEnd, body);
    }

    /**
     * STEP 10: Build complete request for parsing
     */
    private boolean prepareRequest(byte[] headerData, int headerEnd, byte[] body) throws IOException {
        // If body is in file, we need to modify Content-Length header to 0
        // so ParseRequest doesn't expect the body in the byte array
        if (tempBodyFile != null && tempBodyFile.exists()) {
            String headersString = new String(headerData, 0, headerEnd, StandardCharsets.ISO_8859_1);

            // Replace Content-Length with 0
            String modifiedHeaders = headersString.replaceFirst(
                    "(?i)Content-Length:\\s*\\d+",
                    "Content-Length: 0"
            );

            // Rebuild header bytes
            byte[] modifiedHeaderBytes = modifiedHeaders.getBytes(StandardCharsets.ISO_8859_1);

            // Build complete request with modified headers
            ByteArrayOutputStream completeRequest = new ByteArrayOutputStream();
            completeRequest.write(modifiedHeaderBytes);
            completeRequest.write("\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));

            headerBuffer = new ByteArrayOutputStream();
            headerBuffer.write(completeRequest.toByteArray());
        } else {
            // Normal case: headers + body
            ByteArrayOutputStream completeRequest = new ByteArrayOutputStream();
            completeRequest.write(headerData, 0, headerEnd + 4); // Include \r\n\r\n
            completeRequest.write(body);

            headerBuffer = new ByteArrayOutputStream();
            headerBuffer.write(completeRequest.toByteArray());
        }

        return true;
    }

    /**
     * STEP 11: Parse and dispatch request
     */
    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(headerBuffer.toByteArray());
            httpRequest.setConnectionHandler(this);

            // Attach uploaded file if exists
            if (tempBodyFile != null && tempBodyFile.exists()) {
                httpRequest.setUploadedFile(tempBodyFile);
            }

            SessionManager.getInstance().attachSession(httpRequest);
            httpResponse = router.routeRequest(httpRequest, server);
            SessionManager.getInstance().appendSessionCookie(httpRequest, httpResponse);
        } catch (Exception ex) {
            logger.error("Error processing request", ex);
            httpResponse = errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        prepareResponseBuffer();
    }

    /**
     * STEP 12: Write response
     */
    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }

        channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            cleanup();
            return true;
        }
        return false;
    }

    public void close() throws IOException {
        cleanup();
        channel.close();
    }

    // ==================== Helper Methods ====================
    private int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n'
                    && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private String extractBoundary(String contentType) {
        int boundaryIdx = contentType.indexOf("boundary=");
        if (boundaryIdx != -1) {
            return contentType.substring(boundaryIdx + 9).trim();
        }
        return null;
    }

    private File createTempFile() throws IOException {
        File uploadDir = new File(System.getProperty("java.io.tmpdir"), "http_uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        return File.createTempFile("body_", ".tmp", uploadDir);
    }

    private void handleError(HttpStatus status) {
        try {
            cleanup();
            httpResponse = errorHandler.handle(server, status);
            prepareResponseBuffer();
            while (writeBuffer != null && writeBuffer.hasRemaining()) {
                channel.write(writeBuffer);
            }
        } catch (IOException e) {
            logger.error("Error sending error response", e);
        } finally {
            try {
                this.close();
            } catch (IOException ignored) {
            }
        }
        state = ProcessingState.ERROR;
    }

    private void prepareResponseBuffer() {
        byte[] body = httpResponse.getBody() == null ? new byte[0] : httpResponse.getBody();

        String reason = httpResponse.getStatusMessage();
        if (reason == null || reason.isEmpty()) {
            HttpStatus statusEnum = resolveStatus(httpResponse.getStatusCode());
            reason = statusEnum != null ? statusEnum.message : "OK";
        }

        httpResponse.getHeaders().putIfAbsent(
                "Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        httpResponse.getHeaders().putIfAbsent("Connection", "close");
        httpResponse.getHeaders().putIfAbsent("Content-Length", String.valueOf(body.length));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(httpResponse.getStatusCode()).append(" ")
                .append(reason).append("\r\n");
        httpResponse.getHeaders().forEach((k, v)
                -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes();
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    private void cleanup() {
        if (bodyFileStream != null) {
            try {
                bodyFileStream.close();
            } catch (IOException ignored) {
            }
            bodyFileStream = null;
        }

        // Don't delete temp file here - it may be needed by UploadHandler
        // The handler will manage the file lifecycle
        if (bodyBuffer != null) {
            bodyBuffer = new ByteArrayOutputStream();
        }
    }

    public File getUploadedFile() {
        return tempBodyFile;
    }

    public void cleanupTempFile() {
        if (tempBodyFile != null && tempBodyFile.exists()) {
            tempBodyFile.delete();
        }
    }

    private HttpStatus resolveStatus(int code) {
        for (HttpStatus status : HttpStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
