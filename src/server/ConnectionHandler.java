package server;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import routing.Router;
import session.SessionManager;
import util.SonicLogger;

public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private static final int MAX_HEADER_SIZE = 16_384;      // 16KB
    private static final int READ_BUFFER_SIZE = 8_192;      // 8KB

    // Network
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    private ByteBuffer writeBuffer;
    private int lastReadBytes;
    private int lastWriteBytes;

    // Configuration
    private final ServerBlock server;
    private final Router router;
    private final ErrorHandler errorHandler;

    // Request Processing State
    private ProcessingState state = ProcessingState.READING_HEADERS;

    // Header/body accumulation
    private final HttpHeaderReader headerReader = new HttpHeaderReader(MAX_HEADER_SIZE);
    private BodyReceiver bodyReceiver;
    private ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
    private final RequestBytesBuilder requestBytesBuilder = new RequestBytesBuilder();

    // Preserve bytes we didn't consume (NO DROPPING!)
    private byte[] pending = new byte[0];

    // Parsed request info
    private long contentLength = 0;
    private boolean isChunked = false;
    private String requestMethod = null;
    private byte[] rawHeaderBytes = null;

    // Response
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private File tempBodyFile = null;

    private enum ProcessingState {
        READING_HEADERS,
        READING_BODY_TO_MEMORY,
        READING_BODY_TO_FILE,
        READING_CHUNK_SIZE,
        READING_CHUNK_DATA,
        READING_CHUNK_TRAILERS,
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
     * Read data from socket and process based on current state.
     * Returns true when request is fully buffered/decoded and ready for dispatchRequest().
     */
    public boolean read(ServerBlock server) throws IOException {
        int bytesRead = channel.read(readBuffer);
        lastReadBytes = bytesRead;

        // Connection closed by client
        if (bytesRead == -1) {
            cleanupStreamsOnly();
            this.close();
            return false;
        }

        readBuffer.flip();
        byte[] incoming = new byte[readBuffer.remaining()];
        readBuffer.get(incoming);
        readBuffer.clear();

        // Merge pending + incoming (VERY IMPORTANT to avoid dropping bytes)
        byte[] data = concat(pending, incoming);
        pending = new byte[0];

        try {
            return switch (state) {
                case READING_HEADERS -> readHeaders(data, server);
                case READING_BODY_TO_MEMORY, READING_BODY_TO_FILE,
                        READING_CHUNK_SIZE, READING_CHUNK_DATA, READING_CHUNK_TRAILERS -> readBody(data, server);
                case REQUEST_COMPLETE -> true;
                case ERROR -> false;
            };
        } catch (BodyReceiver.TrailerTooLargeException e) {
            logger.error("Error processing request", e);
            handleError(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
            return false;
        } catch (BodyReceiver.BodyTooLargeException e) {
            logger.error("Error processing request", e);
            handleError(HttpStatus.PAYLOAD_TOO_LARGE);
            return false;
        } catch (Exception e) {
            logger.error("Error processing request", e);
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    private boolean readHeaders(byte[] data, ServerBlock server) throws IOException {
        headerReader.feed(data);

        if (headerReader.isTooLarge()) {
            handleError(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
            return false;
        }

        if (!headerReader.isComplete()) {
            pending = headerReader.drainPendingExtra();
            return false;
        }

        rawHeaderBytes = headerReader.getRawHeaderBytes();
        byte[] initialBodyBytes = headerReader.getInitialBodyBytes();

        if (!parseAndInitBody(rawHeaderBytes, server)) {
            return false;
        }

        if (state == ProcessingState.REQUEST_COMPLETE) {
            return true;
        }

        if (initialBodyBytes.length > 0) {
            return readBody(initialBodyBytes, server);
        }

        return false;
    }

    private boolean readBody(byte[] data, ServerBlock server) throws IOException {
        if (bodyReceiver == null) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        int consumed = bodyReceiver.feed(data);
        if (bodyReceiver.getMode() == BodyReceiver.Mode.FIXED_LENGTH && consumed < data.length) {
            pending = Arrays.copyOfRange(data, consumed, data.length);
        }

        if (bodyReceiver.isDone()) {
            return finalizeRequestBytes();
        }

        return false;
    }

    private boolean parseAndInitBody(byte[] headerBytesWithCrlfCrlf, ServerBlock server) throws IOException {
        int headerEnd = findHeaderEnd(headerBytesWithCrlfCrlf);
        if (headerEnd == -1) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        String headersString = new String(headerBytesWithCrlfCrlf, 0, headerEnd, StandardCharsets.ISO_8859_1);
        String[] lines = headersString.split("\r\n");
        if (lines.length == 0) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        // Request line
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }
        requestMethod = requestLine[0].trim().toUpperCase();

        // Reset per-request flags
        contentLength = 0;
        isChunked = false;

        // Parse headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;

            int colonIdx = line.indexOf(':');
            if (colonIdx <= 0) continue;

            String name = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();

            switch (name) {
                case "content-length" -> {
                    try {
                        contentLength = Long.parseLong(value);
                        if (contentLength < 0) throw new NumberFormatException("negative");
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
            }
        }

        logger.info("method=" + requestMethod + " isChunked=" + isChunked + " contentLength=" + contentLength);

        // If POST, must have body framing (Content-Length OR chunked)
        if ("POST".equals(requestMethod)) {
            if (!isChunked && contentLength == 0) {
                handleError(HttpStatus.LENGTH_REQUIRED);
                return false;
            }
        }

        // If chunked + content-length both present: reject (recommended to avoid smuggling ambiguity)
        if (isChunked && contentLength > 0) {
            handleError(HttpStatus.BAD_REQUEST);
            return false;
        }

        // Content-Length early limit check
        if (!isChunked && contentLength > server.getClientMaxBodyBytes()) {
            handleError(HttpStatus.PAYLOAD_TOO_LARGE);
            return false;
        }

        tempBodyFile = null;
        if (isChunked) {
            bodyReceiver = new BodyReceiver(server.getClientMaxBodyBytes());
            bodyReceiver.startChunked();
            state = ProcessingState.READING_CHUNK_SIZE;
            return true;
        }

        if (contentLength == 0) {
            state = ProcessingState.REQUEST_COMPLETE;
            headerBuffer = new ByteArrayOutputStream();
            headerBuffer.write(requestBytesBuilder.build(rawHeaderBytes, false, 0, new byte[0]));
            return true;
        }

        bodyReceiver = new BodyReceiver(server.getClientMaxBodyBytes());
        bodyReceiver.startFixedLength(contentLength);
        state = bodyReceiver.isUsingFile() ? ProcessingState.READING_BODY_TO_FILE
                : ProcessingState.READING_BODY_TO_MEMORY;
        return true;
    }

    private boolean finalizeRequestBytes() throws IOException {
        tempBodyFile = bodyReceiver.getTempFile();
        boolean bodyInFile = tempBodyFile != null && tempBodyFile.exists();
        long finalLen = bodyInFile ? 0 : bodyReceiver.getDecodedLength();
        byte[] bodyBytes = bodyInFile ? null : bodyReceiver.getBodyBytes();

        headerBuffer = new ByteArrayOutputStream();
        headerBuffer.write(requestBytesBuilder.build(rawHeaderBytes, isChunked, finalLen, bodyBytes));

        state = ProcessingState.REQUEST_COMPLETE;
        return true;
    }

    // =========================
    // Dispatch + response write
    // =========================

    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(headerBuffer.toByteArray());
            httpRequest.setConnectionHandler(this);

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

    public boolean write() throws IOException {
        if (writeBuffer == null) return true;

        lastWriteBytes = channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            // After writing response, cleanup and close connection (your server uses Connection: close)
            cleanupStreamsOnly();
            if (state == ProcessingState.ERROR) {
                this.close();
            }
            return true;
        }
        return false;
    }

    public void close() throws IOException {
        cleanupStreamsOnly();
        channel.close();
    }

    public int getLastReadBytes() {
        return lastReadBytes;
    }

    public int getLastWriteBytes() {
        return lastWriteBytes;
    }

    public boolean isReadingHeaders() {
        return state == ProcessingState.READING_HEADERS;
    }

    public void forceError(HttpStatus status) {
        handleError(status);
    }

    // =========================
    // Helpers
    // =========================

    private int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private void handleError(HttpStatus status) {
        try {
            // Build error response, but DO NOT busy-loop write here (NIO-friendly).
            httpResponse = errorHandler.handle(server, status);
            prepareResponseBuffer();
        } catch (Exception e) {
            logger.error("Error preparing error response", e);
        } finally {
            state = ProcessingState.ERROR;
        }
    }

    private void prepareResponseBuffer() {
        byte[] body = (httpResponse.getBody() == null) ? new byte[0] : httpResponse.getBody();

        String reason = httpResponse.getStatusMessage();
        if (reason == null || reason.isEmpty()) {
            HttpStatus statusEnum = resolveStatus(httpResponse.getStatusCode());
            reason = (statusEnum != null) ? statusEnum.message : "OK";
        }

        httpResponse.getHeaders().putIfAbsent(
                "Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        httpResponse.getHeaders().putIfAbsent("Connection", "close");
        httpResponse.getHeaders().putIfAbsent("Content-Length", String.valueOf(body.length));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(httpResponse.getStatusCode()).append(" ").append(reason).append("\r\n");
        httpResponse.getHeaders().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    private void cleanupStreamsOnly() {
        if (bodyReceiver != null) {
            bodyReceiver.close();
        }
        if (headerBuffer != null) {
            headerBuffer.reset();
        }
    }

    public File getUploadedFile() {
        return tempBodyFile;
    }

    public void cleanupTempFile() {
        if (tempBodyFile != null && tempBodyFile.exists()) {
            // best-effort
            tempBodyFile.delete();
        }
    }

    private HttpStatus resolveStatus(int code) {
        for (HttpStatus status : HttpStatus.values()) {
            if (status.code == code) return status;
        }
        return null;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        if (a == null || a.length == 0) return (b == null ? new byte[0] : b);
        if (b == null || b.length == 0) return a;
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
