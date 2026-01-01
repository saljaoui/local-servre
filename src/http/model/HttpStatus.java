package http.model;

public enum HttpStatus {

    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    public final int code;
    public final String message;

    HttpStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
