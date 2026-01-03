package server;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import routing.Router;
import util.SonicLogger;

public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private final ErrorHandler errorHandler;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;
    private final ByteArrayOutputStream rawBytes = new ByteArrayOutputStream();
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private final Router router;
    private final ServerBlock server;

    private boolean headersReceived = false;
    private long expectedContentLength = 0;
    private long totalBytesRead = 0;

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
        System.out.println("[DEBUG] Read " + bytesRead + " bytes from client.");

        if (bytesRead == -1) {
            this.close();
            return false;
        }

        totalBytesRead += bytesRead;

        // Check max body size
        if (totalBytesRead > server.getClientMaxBodyBytes()) {
            try {
                httpResponse = errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
                ByteBuffer buffer = ByteBuffer.wrap(httpResponse.toString().getBytes());
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            } catch (IOException e) {
                logger.error("Failed to send 413 response to client", e);
            }
            this.close();
            return false;
        }

        // Copy data from buffer to rawBytes
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        rawBytes.write(data);
        readBuffer.clear();

        // Parse headers to get Content-Length
        if (!headersReceived) {
            String temp = new String(rawBytes.toByteArray());
            int idx = temp.indexOf("\r\n\r\n");
            if (idx != -1) {
                headersReceived = true;
                for (String line : temp.substring(0, idx).split("\r\n")) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        expectedContentLength = Long.parseLong(line.split(":")[1].trim());
                    }
                }
            }
        }

        // Check if we have complete request
        int headerEnd = indexOf(rawBytes.toByteArray(), "\r\n\r\n".getBytes(), 0);
        int bodySize = rawBytes.size() - (headerEnd + 4);

        return expectedContentLength == 0 || bodySize >= expectedContentLength;
    }

    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(rawBytes.toByteArray());
            httpResponse = router.routeRequest(httpRequest, server);
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
        rawBytes.reset();
        headersReceived = false;
        expectedContentLength = 0;
        totalBytesRead = 0;
        httpRequest = null;
        httpResponse = null;
        writeBuffer = null;
    }

    private static int indexOf(byte[] src, byte[] target, int from) {
        outer: for (int i = from; i <= src.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (src[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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