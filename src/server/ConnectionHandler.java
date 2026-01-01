package server;

import config.model.WebServerConfig.ServerBlock;
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

public class ConnectionHandler {

    // private static final SonicLogger logger =
    // SonicLogger.getLogger(ConnectionHandler.class);
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;
    // We use StringBuilder because modifying Strings (+=) is very slow and
    // memory-heavy
    private final ByteArrayOutputStream rawBytes = new ByteArrayOutputStream();
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private final Router router;
    private final ServerBlock server;

    // Track reading state to handle POST bodies correctly
    private boolean headersReceived = false;
    private long expectedContentLength = 0;
    private long totalBytesRead = 0;
    private boolean isCurrentRequestComplete = false;

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
        readBuffer.clear();

        rawBytes.write(data);

        // clear buffer for next read
        readBuffer.clear();

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

        int headerEnd = indexOf(rawBytes.toByteArray(), "\r\n\r\n".getBytes(), 0);
        int bodySize = rawBytes.size() - (headerEnd + 4);

        return expectedContentLength == 0 || bodySize >= expectedContentLength;
    }

    public void dispatchRequest() {
        try {
            // 1. Parse the request
            httpRequest = ParseRequest.processRequest(rawBytes.toByteArray());
            httpResponse = router.routeRequest(httpRequest, server);
            prepareResponseBuffer();

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
        byte[] body = httpResponse.getBody() == null ? new byte[0] : httpResponse.getBody();

        // Resolve reason phrase: prefer explicit message, then enum lookup, fallback OK
        String reason = httpResponse.getStatusMessage();
        if (reason == null || reason.isEmpty()) {
            HttpStatus statusEnum = resolveStatus(httpResponse.getStatusCode());
            reason = statusEnum != null ? statusEnum.message : "OK";
        }

        // Default headers if missing
        httpResponse.getHeaders().putIfAbsent(
                "Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        httpResponse.getHeaders().putIfAbsent("Connection", "close");
        httpResponse.getHeaders().putIfAbsent("Content-Length", String.valueOf(body.length));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(httpResponse.getStatusCode()).append(" ").append(reason).append("\r\n");
        httpResponse.getHeaders().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes();
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    private void prepareError(int code, String msg) {
        byte[] body = msg.getBytes();
        String h = "HTTP/1.1 " + code + " Error\r\nContent-Length: " + body.length + "\r\n\r\n";
        writeBuffer = ByteBuffer.allocate(h.length() + body.length);
        writeBuffer.put(h.getBytes()).put(body).flip();
    }

    private static int indexOf(byte[] src, byte[] target, int from) {
        outer: for (int i = from; i <= src.length - target.length; i++) {
            for (int j = 0; j < target.length; j++)
                if (src[i + j] != target[j])
                    continue outer;
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
