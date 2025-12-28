package http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple HTTP/1.1 response model to be filled by handlers and written by ResponseWriter.
 */
public class HttpResponse {
    private int status = 200;
    private String statusMessage = "OK";
    private final Map<String, String> headers = new HashMap<>();
    private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
    private boolean keepAlive = true;

    public HttpResponse() {
        setHeader("Server", "SonicServer");
    }

    public void setStatus(int code) { this.status = code; }
    public void setStatusMessage(String msg) { this.statusMessage = msg; }

    public int getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }

    public boolean isKeepAlive() { return keepAlive; }
    public void setKeepAlive(boolean keepAlive) { this.keepAlive = keepAlive; }

    public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }

    public void setHeader(String name, String value) {
        if (name == null) return;
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
    }

    public void addHeader(String name, String value) {
        setHeader(name, value);
    }

    public void setContentType(String contentType) { setHeader("Content-Type", contentType); }

    // Helper to write content easily (UTF-8)
    public void write(String content) {
        if (content == null) return;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        write(bytes);
    }

    public void write(byte[] content) {
        if (content == null) return;
        try { bodyStream.write(content); } catch (Exception e) { }
    }

    public byte[] getBodyBytes() { return bodyStream.toByteArray(); }

    /**
     * Build raw HTTP/1.1 response bytes (headers + body).
     */
    public byte[] getBytes() {
        byte[] body = getBodyBytes();

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(status).append(" ").append(statusMessage).append("\r\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        headerBuilder.append("Content-Length: ").append(body.length).append("\r\n");
        headerBuilder.append("\r\n");

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.UTF_8);

        byte[] fullResponse = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, fullResponse, 0, headerBytes.length);
        System.arraycopy(body, 0, fullResponse, headerBytes.length, body.length);

        return fullResponse;
    }
}
