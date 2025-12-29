package server;

import config.model.WebServerConfig;
import http.HttpParser;
import http.HttpRequest;
import http.HttpResponse;
import routing.Dispatcher;
import util.SonicLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConnectionHandler {
    private static final SonicLogger logger = SonicLogger.getLogger(EventLoop.class);

    private static final int BUFFER_SIZE = 8192;

    private final SocketChannel channel;
    private final SelectionKey key;
    private final WebServerConfig.ServerBlock serverBlock;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final long createdAt;
    private long lastActivity;

    // Connection state
    private State state;
    private final StringBuilder requestData;
    private byte[] responseData;
    private int responsePosition;
    private boolean writeComplete;
    private final Dispatcher dispatcher = new Dispatcher();
    private final HttpParser httpParser = new HttpParser();
    public enum State {
        READING_REQUEST,
        PROCESSING,
        WRITING_RESPONSE,
        CLOSED
    }

    public ConnectionHandler(SocketChannel channel, SelectionKey key, WebServerConfig.ServerBlock serverBlock) {
        this.channel = channel;
        this.key = key;
        this.serverBlock = serverBlock;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
        this.state = State.READING_REQUEST;
        this.requestData = new StringBuilder();
        logger.debug("New connection from: " + getRemoteAddress());
    }

    private boolean checkRequestComplete() {
        String request = requestData.toString();

        // Check for end of headers (\r\n\r\n)
        int headerEnd = request.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            return false; // Headers not fully received yet
        }

        // Determine request type from the start-line
        String startLine = "";
        String[] lines = request.split("\r\n", 2);
        if (lines.length > 0) {
            startLine = lines[0].toUpperCase();
        }

        // Requests that do not include a body are complete once headers are received
        if (startLine.startsWith("GET ") || startLine.startsWith("HEAD ") || startLine.startsWith("DELETE ")
                || startLine.startsWith("OPTIONS ") || startLine.startsWith("TRACE ")) {
            return true;
        }

        // For requests that may have a body (e.g. POST, PUT), check Content-Length
        String[] headers = request.substring(0, headerEnd).split("\r\n");
        int contentLength = 0;

        for (String line : headers) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                    break;
                } catch (NumberFormatException e) {
                    logger.error("Invalid Content-Length header", e);
                    return true; // treat as complete to allow processing of what we have
                }
            }
        }

        if (contentLength <= 0) {
            return true; // no body expected
        }

        int bodyStart = headerEnd + 4; // +4 for \r\n\r\n
        return (request.length() - bodyStart) >= contentLength;
    }

    public boolean read() throws IOException {
        updateActivity();
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }
        if (bytesRead == 0) {
            return false;
        }
        readBuffer.flip();
        // Convert bytes to string and append
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        String receivedData = new String(data);
        requestData.append(receivedData);
        // Clear buffer for next read
        readBuffer.clear();

        // If headers complete, log the header portion for debugging
        int headerEnd = requestData.indexOf("\r\n\r\n");
        if (headerEnd != -1) {
            String headersPart = requestData.substring(0, headerEnd);
            // logger.debug("Received headers:\n" + headersPart);
            String startLine = headersPart.split("\r\n", 2)[0];
            logger.debug("Start-line: " + startLine);
        }

        return checkRequestComplete();
        // return false;
    }

    public boolean write() throws IOException {
        updateActivity();

        // If no write buffer, prepare response
        // Create a String from the byte array using UTF-8 standard
        // String responseString = new String(responseData, StandardCharsets.UTF_8);
        // System.out.println("test ConnectionHandler.write() " + responseString);
        if (responseData == null) {
        }
        prepareResponse();

        // Write as much as possible
        int remaining = responseData.length - responsePosition;
        if (remaining > 0) {
            writeBuffer.clear();
            int bytesToWrite = Math.min(remaining, writeBuffer.capacity());
            writeBuffer.put(responseData, responsePosition, bytesToWrite);
            writeBuffer.flip();

            int bytesWritten = channel.write(writeBuffer);
            responsePosition += bytesWritten;

            logger.debug("Wrote " + bytesWritten + " bytes to " + getRemoteAddress());
        }

        // Check if write is complete
        if (responsePosition >= responseData.length) {
            writeComplete = true;
            return true;
        }

        return false;
    }

    /**
     * Prepare HTTP response
     */
    private void prepareResponse() {
        // For now, just return a simple HTTP response
        String response = 
                "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: 13\r\n"
                + "Connection: " + (shouldKeepAlive() ? "keep-alive" : "close") + "\r\n"
                + "\r\n"
                + "Hello, World!";

        responseData = response.getBytes();
        responsePosition = 0;
        writeComplete = false;
    }

    /**
     * Should keep connection alive?
     */
    public boolean shouldKeepAlive() {
        if (requestData == null) {
            return false;
        }

        String request = requestData.toString();
        String[] headers = request.split("\r\n");

        // Default to HTTP/1.1 behavior (keep-alive by default)
        boolean keepAlive = true;

        // Check Connection header
        for (String header : headers) {
            if (header.toLowerCase().startsWith("connection:")) {
                String connectionValue = header.substring("connection:".length()).trim().toLowerCase();
                if (connectionValue.equals("close")) {
                    keepAlive = false;
                } else if (connectionValue.equals("keep-alive")) {
                    keepAlive = true;
                }
                break;
            }
        }

        return keepAlive;
    }


    public  void processRequest() {
        logger.info("Processing request from " + getRemoteAddress());
        System.out.println("*-- HTTP Request Start ---*");
        // ==========================================
        // STEP 2: PREPARE RESPONSE CONTAINER
        // ==========================================
        HttpRequest request = null;
        try {
            request = httpParser.processRequest(requestData);
        } catch (Exception e) {
             e.printStackTrace();
        }
        HttpResponse response = new HttpResponse();

        // ==========================================
        // STEP 3: DISPATCH (Dynamic Logic)
        // ==========================================
        boolean handledByServlet = dispatcher.dispatch(request, response);

        // ==========================================
        // STEP 4: FALLBACK (Static Files)
        // ==========================================
        if (!handledByServlet) {
            // If no Servlet handled it, try to serve a file from your config
            // ... (Use your existing logic to serve ./www/main/index.html here) ...
            // For now, just 404 if no servlet matches
            response.setStatus(404);
            response.write("<h1>404 Not Found</h1><p>No servlet found for: " + request.getPath() + "</p>");
        }

        // ==========================================
        // STEP 5: PREPARE FOR NIO WRITE
        // ==========================================
        this.responseData = response.getBytes(); // Convert object to bytes
        this.responsePosition = 0;
        this.state = State.WRITING_RESPONSE;
        key.interestOps(SelectionKey.OP_WRITE);
    }
    // Reuse the Content-Type helper from before

    private String getContentType(Path filePath) {
        String fileName = filePath.getFileName().toString();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "text/plain";
    }

    private void sendResponse(int status, String contentType, byte[] body) {
        String header = "HTTP/1.1 " + status + " OK\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        responseData = new byte[header.length() + body.length];
        System.arraycopy(header.getBytes(), 0, responseData, 0, header.length());
        System.arraycopy(body, 0, responseData, header.length(), body.length);

        state = State.WRITING_RESPONSE;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private Path resolveFilePath(String root, String path, String indexFile) {
        try {
            // If user asks for "/", append the index file from config (e.g. "index.html")
            if (path.endsWith("/")) {
                path = path + indexFile;
            }
            Path resolved = Paths.get(root, path).normalize();

            // Security: Ensure we don't go outside the root folder (e.g. ../../)
            if (!resolved.startsWith(Paths.get(root).normalize())) {
                return null;
            }
            return resolved;
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        state = State.CLOSED;
        try {
            logger.debug("Closing connection: " + getRemoteAddress());
            channel.close();
            key.cancel();
        } catch (IOException e) {
            logger.error("Error closing connection", e);
        }
    }

    private void updateActivity() {
        lastActivity = System.currentTimeMillis();
    }

    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastActivity > timeoutMs;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void sendErrorResponse(int statusCode, String message) {
        String response = String.format("""
                HTTP/1.1 %d %s\r
                Content-Type: text/plain\r
                Connection: close\r
                \r
                %d %s""",
                statusCode, message, statusCode, message);

        responseData = response.getBytes();
        responsePosition = 0;
        writeComplete = false;
    }

    private void sendRedirect(int status, String location) {
        String body = "<h1>Redirecting to " + location + "</h1>";
        String header = "HTTP/1.1 " + status + " Moved Permanently\r\n"
                + "Location: " + location + "\r\n"
                + "Content-Length: " + body.length() + "\r\n"
                + "Connection: close\r\n"
                + "\r\n" + body;

        responseData = header.getBytes();
        state = State.WRITING_RESPONSE;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public String getRemoteAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }

    public long getConnectionDuration() {
        return System.currentTimeMillis() - createdAt;
    }
}
