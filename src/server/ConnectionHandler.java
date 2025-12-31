package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import config.model.WebServerConfig.ServerBlock;
import http.HttpParser;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.Router;

public class ConnectionHandler {

    // private static final SonicLogger logger =
    // SonicLogger.getLogger(ConnectionHandler.class);
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;
    // We use StringBuilder because modifying Strings (+=) is very slow and
    // memory-heavy
    private final StringBuilder rawRequestBuilder = new StringBuilder();
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private final Router router;
    private final ServerBlock server;

    // Track reading state to handle POST bodies correctly
    private boolean headersReceived = false;
    private long expectedContentLength = 0;
    private long totalBytesRead = 0;

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
        this.router = new Router();
    }

    /**
     * THE READER
     * 1. Reads bytes from network.
     * 2. Converts to String.
     * 3. Checks if we have enough data (Headers + Body).
     */
    // Read data from client - returns true when complete
    public boolean read() throws IOException {
        int bytesRead = channel.read(readBuffer);
        System.out.println("[DEBUG] Read " + bytesRead + " bytes from client.");
        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }

        // Convert buffer to string
        readBuffer.flip();// Switch to read mode
        byte[] data = new byte[readBuffer.remaining()];

        readBuffer.get(data);

        String chunck = new String(data, StandardCharsets.UTF_8);
        rawRequestBuilder.append(chunck);// Append new data to request builder
        totalBytesRead += bytesRead;// Update total bytes read

        System.out.println("[DEBUG] Total bytes read so far: " + totalBytesRead);

        // clear buffer for next read
        readBuffer.clear();

        if (!headersReceived) {
            if (rawRequestBuilder.toString().contains("\r\n\r\n")) {
                headersReceived = true;
                // Parse headers to find Content-Length
                String headersPart = rawRequestBuilder.toString();
                String[] headerLines = headersPart.split("\r\n");
                for (String line : headerLines) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        String val = line.substring("content-length:".length()).trim();
                        try {
                            expectedContentLength = Long.parseLong(val);
                        } catch (NumberFormatException e) {
                            // Invalid length
                        }
                    }
                }
            }
        }

        if (!headersReceived) {
            return false; // Still waiting for headers
        }
        if (expectedContentLength > 0) {
            int headerEndIndex = rawRequestBuilder.indexOf("\r\n\r\n");
            int headerSize = headerEndIndex + 4;
            long bodySizeSoFar = totalBytesRead - headerSize;

            return bodySizeSoFar >= expectedContentLength;
        }
        // HTTP request ends with double newline
        return true;
    }

    public void dispatchRequest() {
        try {
            // 1. Parse the request
            httpRequest = ParseRequest.processRequest(rawRequestBuilder.toString());
            String contentLength = httpRequest.getHeaders().get("Content-Length");
            if (contentLength != null) {
                try {
                    long contentLen = Long.parseLong(contentLength);
                    long contentBodyLength = server.getClientMaxBodyBytes();
                    if (contentLen > contentBodyLength) {
                        System.err.println("413 Payload Too Large");
                        prepareError(413, "Payload Too Large");
                        return;
                    }

                } catch (Exception e) {
                    System.err.println("400 Bad Request - Invalid Content-Length");
                    prepareError(400, "Bad Request");
                    return;
                }
            }
            // 2. Route the request to get a proper HttpResponse
            httpResponse = router.routeRequest(httpRequest, server);
            prepareResponseBuffer();
            // 4. Prepare Output Buffer

        } catch (Exception ex) {
            System.getLogger(ConnectionHandler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            prepareError(500, "Internal Server Error");
        }
    }

    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }

        channel.write(writeBuffer);
        return !writeBuffer.hasRemaining();
    }

    public void close() throws IOException {
        channel.close();
    }

    private void prepareResponseBuffer() {
        // 3. Build raw HTTP response bytes from HttpResponse object
        StringBuilder responseBuilder = new StringBuilder();

        // Status line
        responseBuilder.append("HTTP/1.1 ")
                .append(httpResponse.getStatusCode())
                .append(" ")
                .append(httpResponse.getStatusMessage())
                .append("\r\n");

        // Headers
        if (httpResponse.getHeaders() != null) {
            httpResponse.getHeaders().forEach((k, v) -> {
                responseBuilder.append(k).append(": ").append(v).append("\r\n");
            });
        }
        if (!httpResponse.getHeaders().containsKey("Access-Controller-All-Origin")) {
            responseBuilder.append("Access-Control-Allow-Origin: *\r\n");
        }
        if (!httpResponse.getHeaders().containsKey("Access-Control-Allow-Methods")) {
            responseBuilder.append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        }

        if (!httpResponse.getHeaders().containsKey("Access-Control-Allow-Headers")) {
            responseBuilder.append("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        }

        byte[] body = httpResponse.getBody() != null ? httpResponse.getBody() : new byte[0];

        // Auto-add Content-Length if missing
        if (!httpResponse.getHeaders().containsKey("Content-Length")) {
            responseBuilder.append("Content-Length: ").append(body.length).append("\r\n");
        }
        responseBuilder.append("\r\n");

        byte[] headerBytes = responseBuilder.toString().getBytes(StandardCharsets.UTF_8);

        // Combine headers + body into writeBuffer
        writeBuffer = ByteBuffer.allocate(headerBytes.length + body.length);
        writeBuffer.put(headerBytes);
        writeBuffer.put(body);
        writeBuffer.flip(); // Prepare buffer for writing
    }

    private void prepareError(int code, String message) {
        String html = "<h1>" + code + " " + message + "</h1>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);

        String header = "HTTP/1.1 " + code + " Error\r\n"
                + "Content-Type: text/html\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";

        writeBuffer = ByteBuffer.allocate(header.length() + body.length);
        writeBuffer.put(header.getBytes(StandardCharsets.UTF_8));
        writeBuffer.put(body);
        writeBuffer.flip();
    }
}
