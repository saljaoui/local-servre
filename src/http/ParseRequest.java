package http;

import http.model.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ParseRequest {

    public static HttpRequest processRequest(byte[] raw) throws Exception {
        HttpRequest req = new HttpRequest();
        byte[] sep = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
        
        int headerEnd = indexOf(raw, sep, 0);
        if (headerEnd == -1) throw new Exception("Bad HTTP: Missing header separator");

        String headers = new String(raw, 0, headerEnd, StandardCharsets.ISO_8859_1);
        String[] lines = headers.split("\r\n");

        if (lines.length == 0) throw new Exception("Bad HTTP: Empty request");

        parseRequestLine(lines[0], req);
        for (int i = 1; i < lines.length; i++) {
            parseHeaderLine(lines[i], req);
        }

        // Validate Host header for HTTP/1.1
        if (req.getHttpVersion().equals("HTTP/1.1") && req.getHeader("Host") == null) {
            throw new Exception("Host header required for HTTP/1.1");
        }

        // Parse body
        int bodyStart = headerEnd + 4;
        byte[] bodyBytes = bodyStart < raw.length
                ? Arrays.copyOfRange(raw, bodyStart, raw.length)
                : new byte[0];

        // Validate Content-Length if present
        String contentLength = req.getHeader("Content-Length");
        if (contentLength != null) {
            int expectedLength = Integer.parseInt(contentLength);
            if (bodyBytes.length < expectedLength) {
                throw new Exception("Incomplete body: expected " + expectedLength + 
                                  ", got " + bodyBytes.length);
            }
            // Trim body to exact Content-Length
            bodyBytes = Arrays.copyOf(bodyBytes, expectedLength);
        }

        req.setBody(bodyBytes);
        return req;
    }

    private static void parseRequestLine(String line, HttpRequest request) {
        String[] p = line.split(" ");
        request.setMethod(p[0]);
        request.setUri(p[1]);
        request.setPath(p[1]);
        request.setHttpVersion(p.length > 2 ? p[2] : "HTTP/1.1");

    }

    private static void parseHeaderLine(String line, HttpRequest request) {
        int colonIndex = line.indexOf(':');
        if (colonIndex <= 0) return;

        String name = line.substring(0, colonIndex).trim();
        String value = line.substring(colonIndex + 1).trim();

        request.setHeaders(name, value);

        if (name.equalsIgnoreCase("Cookie")) {
            parseCookieHeader(value, request);
        }
    }

    private static void parseCookieHeader(String cookieValue, HttpRequest request) {
        if (cookieValue == null || cookieValue.isEmpty()) return;

        String[] cookies = cookieValue.split(";");
        for (String cookie : cookies) {
            int equalIndex = cookie.indexOf('=');
            if (equalIndex > 0) {
                String name = cookie.substring(0, equalIndex).trim();
                String value = cookie.substring(equalIndex + 1).trim();
                request.addCookie(name, value);
            }
        }
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        outer: for (int i = fromIndex; i < source.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}