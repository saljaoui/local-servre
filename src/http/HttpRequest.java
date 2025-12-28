package http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class HttpRequest {
    private String method;
    private String path;
    private String httpVersion;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private byte[] body;

    public HttpRequest() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    public String getQuery() {
        if (path == null)
            return "";
        int queryIndex = path.indexOf('?');
        return (queryIndex == -1) ? "" : path.substring(queryIndex + 1);
    }

    public void addQueryParam(String name, String value) {
        queryParams.put(name, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getBodyAsString() {
        return body == null ? null : new String(body);
    }

    public String getParameter(String name) {
        return queryParams.get(name);
    }

    /**
     * Convenience: get parameter from query string or urlencoded body.
     */
    public void parseParameters(String queryString) {
        if (queryString == null || queryString.isEmpty())
            return;
        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                queryParams.put(kv[0], kv[1]);
            }
        }
    }
}
