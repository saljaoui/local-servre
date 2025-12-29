package http;

import http.model.HttpRequest;
import server.ConnectionHandler;
import util.SonicLogger;

import java.util.HashMap;
import java.util.Map;

public class ParseRequest {
    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    public static HttpRequest parseRequest(String rawRequest) {
        HttpRequest request = new HttpRequest();

        // Split headers and body
        String[] parts = rawRequest.split("\r\n\r\n", 2);
        String headerPart = parts[0];
        String bodyPart = parts.length > 1 ? parts[1] : "";

        String[] lines = headerPart.split("\r\n");

        // ---- Request line ----
        // Example: GET /index.html HTTP/1.1
        String[] requestLine = lines[0].split(" ");

        if (requestLine.length >= 2) {
            request.setMethod(requestLine[0]);
            request.setPath(requestLine[1]);
        }

        // ---- Headers ----
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }
        request.setHeaders(headers);

        // ---- Body (POST / PUT) ----
        if (!bodyPart.isEmpty()) {
            request.setBody(bodyPart.getBytes());
        } else {
            request.setBody(new byte[0]);
        }

        return request;
    }
}
