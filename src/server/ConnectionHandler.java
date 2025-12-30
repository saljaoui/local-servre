package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import http.HttpParser;
import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.Router;
import util.SonicLogger;

public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer writeBuffer;
    private String request = "";
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private Router router;

    public ConnectionHandler(SocketChannel channel) {
        this.channel = channel;
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
        System.out.println("Processing request...");

        // 1. Parse the request
        try {
            httpRequest = HttpParser.processRequest(request);
        } catch (Exception e) {
            //   Auto-generated catch block
            e.printStackTrace();
        }

        // 2. Route the request to get a proper HttpResponse
        httpResponse = router.routeRequest(httpRequest);

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

    // READ YOUR HTML FILE - This loads index.html
    private String buildYourHtml() {
        try {
            // Path to your HTML file - change this if needed!
            java.nio.file.Path filePath = java.nio.file.Paths.get("www/main/index.html");

            // Read all bytes from file
            byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

            // Convert to String with UTF-8 encoding
            String html = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);

            return html;

        } catch (IOException e) {

            return "<html><body>" +
                    "<h1>Error 404 - File Not Found</h1>" +
                    "<p>Could not load <code>www/main/index.html</code></p>" +
                    "<p>Make sure the file exists in the correct location.</p>" +
                    "</body></html>";
        }
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