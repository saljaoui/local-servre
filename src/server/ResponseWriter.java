package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import http.model.HttpResponse;
import http.model.HttpStatus;

public class ResponseWriter {
    private final SocketChannel channel;
    private ByteBuffer writeBuffer;

    public ResponseWriter(SocketChannel channel) {
        this.channel = channel;
    }

    public void prepare(HttpResponse response) {
        byte[] body = response.getBody() == null ? new byte[0] : response.getBody();

        String reason = response.getStatusMessage();
        if (reason == null || reason.isEmpty()) {
            HttpStatus statusEnum = resolveStatus(response.getStatusCode());
            reason = statusEnum != null ? statusEnum.message : "OK";
        }

        response.getHeaders().putIfAbsent(
                "Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        response.getHeaders().putIfAbsent("Connection", "close");
        response.getHeaders().putIfAbsent("Content-Length", String.valueOf(body.length));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(response.getStatusCode()).append(" ").append(reason).append("\r\n");
        response.getHeaders().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes();
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }

        channel.write(writeBuffer);
        return !writeBuffer.hasRemaining();
    }

    public void reset() {
        writeBuffer = null;
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