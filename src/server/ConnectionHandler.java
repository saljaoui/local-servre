package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import config.model.WebServerConfig.ServerBlock;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.Router;

public class ConnectionHandler {

    // private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer writeBuffer;
    private String request = "";
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private Router router;
    private ServerBlock server;

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
        this.router = new Router();
    }

    // Read data from client - returns true when complete
    public boolean read() throws IOException {
        int bytesRead = channel.read(readBuffer);

        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }

        // Convert buffer to string
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        request += new String(data);
        readBuffer.clear();

        // HTTP request ends with double newline
        return request.contains("\r\n\r\n");
    }

    // Process the HTTP request and prepare response
    public void dispatchRequest() {
        // 1. Parse the request
       httpRequest = HttpParser.processRequest(request);

        // 2. Route the request to get a proper HttpResponse
        httpResponse = router.routeRequest(httpRequest, server);

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

        // Add Content-Length if not already set
        if (!httpResponse.getHeaders().containsKey("Content-Length")) {
            int length = httpResponse.getBody() != null ? httpResponse.getBody().length : 0;
            responseBuilder.append("Content-Length: ").append(length).append("\r\n");
        }

        // End headers
        responseBuilder.append("\r\n");

        // Body
        byte[] body = httpResponse.getBody() != null ? httpResponse.getBody() : new byte[0];
        byte[] headerBytes = responseBuilder.toString().getBytes();

        // Combine headers + body into writeBuffer
        writeBuffer = ByteBuffer.allocate(headerBytes.length + body.length);
        writeBuffer.put(headerBytes);
        writeBuffer.put(body);
        writeBuffer.flip(); // Prepare buffer for writing
    }

    // Write response to client - returns true when complete
    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }

        channel.write(writeBuffer);

        // Return true when all data is written
        return !writeBuffer.hasRemaining();
    }

    // Close the connection
    public void close() throws IOException {
        channel.close();
    }
}