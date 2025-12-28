package http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple HTTP/1.1 response model to be filled by handlers and written by ResponseWriter.
 */
public class HttpResponse {
    private int statusCode = 200;
    private String reasonPhrase = "OK";
    private boolean keepAlive = true;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private byte[] body;

    public HttpResponse() {
        addHeader("Server", "SonicServer");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatus(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeader(String name, String value) {
        List<String> vals = new ArrayList<>();
        vals.add(value);
        headers.put(name, vals);
    }

    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name, __ -> new ArrayList<>()).add(value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
        if (body != null) {
            setHeader("Content-Length", String.valueOf(body.length));
        }
    }

    public void setBody(String s) {
        if (s == null) {
            setBody((byte[]) null);
        } else {
            setBody(s.getBytes());
        }
    }

    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    public String getBodyAsString() {
        return body == null ? null : new String(body);
    }
}
