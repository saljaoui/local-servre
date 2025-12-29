package http.model;

import java.util.Map;

public class HttpRequest {

    private String method;
    private String path;
    private Map<String, String> headers;
    private byte[] body;

    public HttpRequest() {
    }

    // Getters
    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    // Setters
    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    // Helper: get cookie value by name
    public String getCookie(String name) {
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String c : cookies) {
                String[] parts = c.trim().split("=");
                if (parts.length == 2 && parts[0].equals(name)) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", bodyLength=" + (body != null ? body.length : 0) +
                '}';
    }
}
