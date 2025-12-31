package http.model;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String method;
    private String uri;
    private String path;
    private String queryString;
    private String httpVersion;
    private Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final long contentLength;
    private   Map<String, String> cookies;
     

    private byte[] body;

    public HttpRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.cookies = new HashMap<>();
        this.body = new byte[0];
        this.queryString = "";
        this.httpVersion = "";
        this.contentLength = 0;
    }

    // Getters
    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public String getPath() { return path; }
    public String getQueryString() { return queryString; }
    public String getHttpVersion() { return httpVersion; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public Map<String, String> getCookies() { return cookies; }
    public byte[] getBody() { return body; }

    // Setters
    public void setMethod(String method) { this.method = method; }
    public void setUri(String uri) { this.uri = uri; }
    public void setPath(String path) { this.path = path; }
    public void setQueryString(String qs) { this.queryString = qs; }
    public void setHttpVersion(String v) { this.httpVersion = v; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setBody(byte[] body) { this.body = body; }

    // Helpers
    public void addHeader(String name, String value) { this.headers.put(name, value); }
    // Some code uses setHeaders(name, value) pattern; keep compatibility
    public void setHeaders(String name, String value) { addHeader(name, value); }
    public String getHeader(String name) { return this.headers.get(name); }

    public void addQueryParam(String key, String value) { this.queryParams.put(key, value); }
    public String getQueryParam(String key) { return this.queryParams.get(key); }

    public void addCookie(String name, String value) { this.cookies.put(name, value); }
    public String getCookie(String name) { return this.cookies.get(name); }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", path='" + path + '\'' +
                ", queryString='" + queryString + '\'' +
                ", headers=" + headers +
                ", queryParams=" + queryParams +
                ", cookies=" + cookies +
                ", bodyLength=" + (body != null ? body.length : 0) +
                '}';
    }
}