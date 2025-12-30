package handlers;

import http.model.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ErrorHandler {

    private final String errorPagesRoot = "error_pages";

    //  Generic method to handle all error responses
    private HttpResponse buildErrorResponse(int statusCode, String statusMessage, String errorPageFileName) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(statusCode);
        response.setStatusMessage(statusMessage);

        File file = new File(errorPagesRoot + "/" + errorPageFileName);

        if (file.exists() && !file.isDirectory()) {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                response.setBody(content);

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "text/html; charset=UTF-8");
                headers.put("Content-Length", String.valueOf(content.length));
                response.setHeaders(headers);

            } catch (IOException e) {
                setEmptyBody(response);
            }
        } else {
            setEmptyBody(response);
        }

        return response;
    }

    //  Helper method to set an empty body with appropriate headers
    private void setEmptyBody(HttpResponse response) {
        response.setBody(new byte[0]);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Length", "0");
        response.setHeaders(headers);
    }

    //  400 Bad Request
    public HttpResponse badRequest() {
        return buildErrorResponse(400, "Bad Request", "400.html");
    }

    //  403 Forbidden
    public HttpResponse forbidden() {
        return buildErrorResponse(403, "Forbidden", "403.html");
    }

    //  404 Not Found
    public HttpResponse notFound() {
        return buildErrorResponse(404, "Not Found", "404.html");
    }

    //  405 Method Not Allowed
    public HttpResponse methodNotAllowed() {
        return buildErrorResponse(405, "Method Not Allowed", "405.html");
    }

    //  413 Payload Too Large
    public HttpResponse payloadTooLarge() {
        return buildErrorResponse(413, "Payload Too Large", "413.html");
    }

    //  500 Internal Server Error
    public HttpResponse internalServerError() {
        return buildErrorResponse(500, "Internal Server Error", "500.html");
    }

    //  Generic error handler for any status code
    public HttpResponse handleError(int statusCode, String statusMessage) {
        String fileName = statusCode + ".html";
        return buildErrorResponse(statusCode, statusMessage, fileName);
    }
}