package http.model;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private byte[] body;

    public HttpResponse() {
        this.headers = new HashMap<>();
        this.body = new byte[0];
    }

    // Getters
    public int getStatusCode() { return statusCode; }
    public String getStatusMessage() { return statusMessage; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    // Setters
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public void setStatus(HttpStatus status) {
        this.statusCode = status.code;
        this.statusMessage = status.message;
    }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setBody(byte[] body) { this.body = body; }

    // Helper: add single header
    public void addHeader(String name, String value) { this.headers.put(name, value); }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "statusCode=" + statusCode +
                ", statusMessage='" + statusMessage + '\'' +
                ", headers=" + headers +
                ", bodyLength=" + (body != null ? body.length : 0) +
                '}';
    }
}
