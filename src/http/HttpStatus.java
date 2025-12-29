package http;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP status codes and reason phrases
 */
public class HttpStatus {
    
    // Success
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    
    // Redirection
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    public static final int NOT_MODIFIED = 304;
    
    // Client errors
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int PAYLOAD_TOO_LARGE = 413;
    
    // Server errors
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;
    public static final int SERVICE_UNAVAILABLE = 503;
    
    private static final Map<Integer, String> REASON_PHRASES = new HashMap<>();
    
    static {
        // Success
        REASON_PHRASES.put(200, "OK");
        REASON_PHRASES.put(201, "Created");
        REASON_PHRASES.put(204, "No Content");
        
        // Redirection
        REASON_PHRASES.put(301, "Moved Permanently");
        REASON_PHRASES.put(302, "Found");
        REASON_PHRASES.put(304, "Not Modified");
        
        // Client errors
        REASON_PHRASES.put(400, "Bad Request");
        REASON_PHRASES.put(401, "Unauthorized");
        REASON_PHRASES.put(403, "Forbidden");
        REASON_PHRASES.put(404, "Not Found");
        REASON_PHRASES.put(405, "Method Not Allowed");
        REASON_PHRASES.put(408, "Request Timeout");
        REASON_PHRASES.put(413, "Payload Too Large");
        
        // Server errors
        REASON_PHRASES.put(500, "Internal Server Error");
        REASON_PHRASES.put(501, "Not Implemented");
        REASON_PHRASES.put(503, "Service Unavailable");
    }
    
    public static String getReasonPhrase(int statusCode) {
        return REASON_PHRASES.getOrDefault(statusCode, "Unknown");
    }
    
    public static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
    
    public static boolean isRedirection(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }
    
    public static boolean isClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public static boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }
}