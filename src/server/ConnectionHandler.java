package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import config.model.WebServerConfig.ServerBlock;
import http.HttpParser;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.Router;

public class ConnectionHandler {

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

    public boolean read() throws IOException {
        int bytesRead = channel.read(readBuffer);

        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        request += new String(data);
        readBuffer.clear();
        return request.contains("\r\n\r\n");
    }

    public void dispatchRequest() {
        try {
            httpRequest = HttpParser.processRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        httpResponse = router.routeRequest(httpRequest, server);
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("HTTP/1.1 ")
                .append(httpResponse.getStatusCode())
                .append(" ")
                .append(httpResponse.getStatusMessage())
                .append("\r\n");
        if (httpResponse.getHeaders() != null) {
            httpResponse.getHeaders().forEach((k, v) -> {
                responseBuilder.append(k).append(": ").append(v).append("\r\n");
            });
        }
        if (!httpResponse.getHeaders().containsKey("Content-Length")) {
            int length = httpResponse.getBody() != null ? httpResponse.getBody().length : 0;
            responseBuilder.append("Content-Length: ").append(length).append("\r\n");
        }
        responseBuilder.append("\r\n");
        byte[] body = httpResponse.getBody() != null ? httpResponse.getBody() : new byte[0];
        byte[] headerBytes = responseBuilder.toString().getBytes();
        writeBuffer = ByteBuffer.allocate(headerBytes.length + body.length);
        writeBuffer.put(headerBytes);
        writeBuffer.put(body);
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
}