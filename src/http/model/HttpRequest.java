package http.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import server.ConnectionHandler;

public class HttpRequest {

    private String method;
    private String uri;
    private String path;
    private String queryString;
    private String httpVersion;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> cookies;
    private byte[] body;
    private File uploadedFile;
    private ConnectionHandler connectionHandler;
    private String sessionId;
    private Map<String, String> sessionData;
    private boolean newSession;

    public HttpRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.cookies = new HashMap<>();
        this.body = new byte[0];
        this.queryString = "";
        this.httpVersion = "";
    }

    // ========== FILE UPLOAD METHODS ==========
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public File getUploadedFile() {
        return uploadedFile;
    }

    public void setConnectionHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    // ========== GETTERS ==========
    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public Map<String, String> getSession() {
        return sessionData;
    }

    public byte[] getBody() {
        return body != null ? body : new byte[0];
    }

    // ========== SETTERS ==========
    public void setMethod(String method) {
        this.method = method;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setQueryString(String qs) {
        this.queryString = qs;
    }

    public void setHttpVersion(String v) {
        this.httpVersion = v;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setSessionData(Map<String, String> sessionData) {
        this.sessionData = sessionData;
    }

    public void setNewSession(boolean newSession) {
        this.newSession = newSession;
    }

    // ========== HELPER METHODS ==========
    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public void setHeaders(String name, String value) {
        addHeader(name, value);
    }

    public String getHeader(String name) {
        if (name == null) {
            return null;
        }
        String direct = this.headers.get(name);
        if (direct != null) {
            return direct;
        }
        String lower = name.toLowerCase();
        String lowerMatch = this.headers.get(lower);
        if (lowerMatch != null) {
            return lowerMatch;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void addQueryParam(String key, String value) {
        this.queryParams.put(key, value);
    }

    public String getQueryParam(String key) {
        return this.queryParams.get(key);
    }

    public void addCookie(String name, String value) {
        this.cookies.put(name, value);
    }

    public String getCookie(String name) {
        return this.cookies.get(name);
    }

    @Override
    public String toString() {
        return "HttpRequest{"
                + "method='" + method + '\''
                + ", uri='" + uri + '\''
                + ", path='" + path + '\''
                + ", queryString='" + queryString + '\''
                + ", headers=" + headers
                + ", queryParams=" + queryParams
                + ", cookies=" + cookies
                + ", bodyLength=" + (body != null ? body.length : 0)
                + '}';
    }

  


    public void setUploadedFile(File uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
}
