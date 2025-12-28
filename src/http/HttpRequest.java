package http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String uri;
    private String path; // Clean path without query
    private String queryString;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> params = new HashMap<>(); // Query params
    private String body;

    // Getters & Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getQueryString() { return queryString; }
    public void setQueryString(String qs) { this.queryString = qs; }

    public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getParameter(String name) { return params.get(name); }
    public void setParameter(String key, String val) { this.params.put(key, val); }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}