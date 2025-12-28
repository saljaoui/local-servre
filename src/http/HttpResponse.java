package http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int status = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

    public HttpResponse() {
        headers.put("Connection", "close"); // Default for simplicity
    }

    public void setStatus(int code) { this.status = code; }
    public void setStatusMessage(String msg) { this.statusMessage = msg; }
    
    public void setContentType(String type) { 
        headers.put("Content-Type", type); 
    }

    // Helper to write content easily
    public void write(String content) {
        bodyStream.write(content.getBytes(StandardCharsets.UTF_8), 0, content.length());
    }
    
    public void write(byte[] content) {
        try { bodyStream.write(content); } catch (Exception e) {}
    }

    // Converts the object to raw bytes for the NIO channel
    public byte[] getBytes() {
        byte[] body = bodyStream.toByteArray();
        
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(status).append(" ").append(statusMessage).append("\r\n");
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        
        headerBuilder.append("Content-Length: ").append(body.length).append("\r\n");
        headerBuilder.append("\r\n");

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.UTF_8);
        
        // Combine header + body
        byte[] fullResponse = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, fullResponse, 0, headerBytes.length);
        System.arraycopy(body, 0, fullResponse, headerBytes.length, body.length);
        
        return fullResponse;
    }
}