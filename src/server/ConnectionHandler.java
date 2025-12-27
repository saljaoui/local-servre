package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import utils.Logger;

public class ConnectionHandler {
    private static final String TAG = "Connection";
    private static final int BUFFER_SIZE = 8192;

    private final SocketChannel channel;
    private final SelectionKey key;
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

    public enum State {
        READING_REQUEST,
        PROCESSING,
        WRITING_RESPONSE,
        CLOSED
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
                    Logger.error(TAG, "Invalid Content-Length header", e);
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

    public ConnectionHandler(SocketChannel channel, SelectionKey key) {
        this.channel = channel;
        this.key = key;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
        this.state = State.READING_REQUEST;
        this.requestData = new StringBuilder();

        Logger.debug(TAG, "New connection from: " + getRemoteAddress());
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
        // DEBUG: Print the received data
        System.out.println("=== DEBUG: Received Data ===");
        System.out.println(receivedData);
        System.out.println("=== END DEBUG ===");

        readBuffer.clear();
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
            System.out.println("ConnectionHandler.write()");
            prepareResponse();
        }

        // Write as much as possible
        int remaining = responseData.length - responsePosition;
        if (remaining > 0) {
            writeBuffer.clear();
            int bytesToWrite = Math.min(remaining, writeBuffer.capacity());
            writeBuffer.put(responseData, responsePosition, bytesToWrite);
            writeBuffer.flip();

            int bytesWritten = channel.write(writeBuffer);
            responsePosition += bytesWritten;

            Logger.debug(TAG, "Wrote " + bytesWritten + " bytes to " + getRemoteAddress());
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
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 13\r\n" +
                "Connection: " + (shouldKeepAlive() ? "keep-alive" : "close") + "\r\n" +
                "\r\n" +
                "Hello, World!";

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

    private boolean isRequestComplete() {
        String data = requestData.toString();
        // Simple check: headers end with \r\n\r\n
        // Handle body for POST requests (Content-Length)
        return data.contains("\r\n\r\n");
    }

    public void processRequest() {
        Logger.info(TAG, "Processing request from " + getRemoteAddress());

        // DEBUG: Print the complete request
        System.out.println("=== DEBUG: Complete Request ===");
        System.out.println(requestData.toString());
        System.out.println("=== END DEBUG ===");

        // Parse request and route to handlers
        // For now, send a simple response
        String requestStr = requestData.toString();
        String path = "/"; 
  // Extract path from request (first line: "GET /path HTTP/1.1")
        String[] requestLines = requestStr.split("\r\n");
        if (requestLines.length > 0) {
            String[] requestParts = requestLines[0].split(" ");
            if (requestParts.length > 1) {
                path = requestParts[1];
             }
        }



        // Create response based on path
        String body = switch (path) {
            case "/" -> "<html><body><h1>Hello from LocalServer!</h1><p>Server is running.</p></body></html>";
            case "/welcome" -> "<html><body><h1>Welcome to our server!</h1></body></html>";
            default -> "<html><body><h1>404 Not Found</h1><p>The requested path was not found: " + path
                                    + "</p></body></html>";
        };
        // Create HTTP response
        String response = """
                          HTTP/1.1 200 OK\r
                          Content-Type: text/html\r
                          Content-Length: """ + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        responseData = response.getBytes();
        responsePosition = 0;
        state = State.WRITING_RESPONSE;

        // Change interest to write
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void close() {
        state = State.CLOSED;
        try {
            Logger.debug(TAG, "Closing connection: " + getRemoteAddress());
            channel.close();
            key.cancel();
        } catch (IOException e) {
            Logger.error(TAG, "Error closing connection", e);
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
        String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        "%d %s",
                statusCode, message, statusCode, message);

        responseData = response.getBytes();
        responsePosition = 0;
        writeComplete = false;
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