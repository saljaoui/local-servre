package http;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Simple data holder for a parsed HTTP/1.1 request.
 * Parsing lives elsewhere; routing/handlers consume this.
 */
public class HttpRequest {
    // Start line
    private String method;
    private String rawTarget;
    private String path;
    private String uri;
    private String queryString;
    private String query;
    private String httpVersion;

    // Connection/meta
    private String host;
    private int hostPort = -1;
    private boolean keepAlive;
    private boolean chunked;
    private long contentLength = -1;
    private String contentType;
    private String transferEncoding;

    // Headers and cookies
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final Map<String, String> cookies = new LinkedHashMap<>();

    // Query params
    private final Map<String, List<String>> queryParams = new LinkedHashMap<>();

    // Body and client info
    private byte[] body;
    private SocketAddress remoteAddress;

    // Getters & Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
     
    
    public String getQueryString() { return queryString; }
    public void setQueryString(String qs) { this.queryString = qs; }

 
    public String getRawTarget() { return rawTarget; }
    public void setRawTarget(String rawTarget) { this.rawTarget = rawTarget; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getHttpVersion() { return httpVersion; }
    public void setHttpVersion(String httpVersion) { this.httpVersion = httpVersion; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getHostPort() { return hostPort; }
    public void setHostPort(int hostPort) { this.hostPort = hostPort; }

    public boolean isKeepAlive() { return keepAlive; }
    public void setKeepAlive(boolean keepAlive) { this.keepAlive = keepAlive; }

    public boolean isChunked() { return chunked; }
    public void setChunked(boolean chunked) { this.chunked = chunked; }

    public long getContentLength() { return contentLength; }
    public void setContentLength(long contentLength) { this.contentLength = contentLength; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTransferEncoding() { return transferEncoding; }
    public void setTransferEncoding(String transferEncoding) { this.transferEncoding = transferEncoding; }

    public SocketAddress getRemoteAddress() { return remoteAddress; }
    public void setRemoteAddress(SocketAddress remoteAddress) { this.remoteAddress = remoteAddress; }

    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }

    // Headers API
    public Map<String, List<String>> getHeaders() { return Collections.unmodifiableMap(headers); }

    public void addHeader(String name, String value) {
        String key = normalize(name);
        headers.computeIfAbsent(key, __ -> new ArrayList<>()).add(value);
    }

    public String header(String name) {
        List<String> vals = headers.get(normalize(name));
        return (vals == null || vals.isEmpty()) ? null : vals.get(0);
    }

    public List<String> headerValues(String name) {
        List<String> vals = headers.get(normalize(name));
        return vals == null ? List.of() : Collections.unmodifiableList(vals);
    }

    // Cookies API
    public Map<String, String> getCookies() { return Collections.unmodifiableMap(cookies); }
    public void addCookie(String name, String value) { cookies.put(name, value); }
    public String cookie(String name) { return cookies.get(name); }

    // Query params API
    public Map<String, List<String>> getQueryParams() { return Collections.unmodifiableMap(queryParams); }
    public void addQueryParam(String name, String value) {
        queryParams.computeIfAbsent(name, __ -> new ArrayList<>()).add(value);
    }
    public String param(String name) {
        List<String> vals = queryParams.get(name);
        return (vals == null || vals.isEmpty()) ? null : vals.get(0);
    }
    public List<String> paramValues(String name) {
        List<String> vals = queryParams.get(name);
        return vals == null ? List.of() : Collections.unmodifiableList(vals);
    }

    public boolean isMethod(String candidate) {
        return method != null && method.equalsIgnoreCase(candidate);
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
