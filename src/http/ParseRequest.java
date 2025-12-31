package http;

import java.nio.charset.StandardCharsets;

import http.model.HttpRequest;
import util.SonicLogger;

public class ParseRequest {
    private static final SonicLogger logger = SonicLogger.getLogger(ParseRequest.class);

    // Requirement: Parse HTTP messages manually
    private static final String HEADER_SEPARATOR = "\r\n\r\n";

    /**
     * Parse complete HTTP request from string.
     * Handles: Request Line, Headers (Host, Cookie, Content-Length), Body.
     */
    public static HttpRequest processRequest(String requestData) throws Exception {
        System.out.println("[CH] [PARSER] Starting Parse. Input String Length: " + requestData.length());
        HttpRequest request = new HttpRequest();

        // 1. Split Header and Body
        int headerEndIndex = requestData.indexOf(HEADER_SEPARATOR);

        if (headerEndIndex == -1) {
            throw new Exception("Invalid HTTP request: no header separator");
        }

        String headerSection = requestData.substring(0, headerEndIndex);
        String bodySection = requestData.substring(headerEndIndex + 4);
        System.out.println("[CH] [PARSER] Body String Length: " + bodySection.length());

        // 2. Split Headers into lines
        String[] linesHeader = headerSection.split("\r\n");

        parseRequestLine(linesHeader[0], request);

        // Lines 1..N are Headers
        for (int i = 1; i < linesHeader.length; i++) {
            parseHeaderLine(linesHeader[i], request);
        }

        // 3. Handle Body (Mandatory for Uploads/POST)
        if (!bodySection.isEmpty()) {
            // =================================================================
            // CRITICAL FIX: DO NOT USE UTF-8 for Binary Files
            // =================================================================
            // Use ISO_8859-1 which maps 1-to-1 byte.
            // Using UTF-8 here is what corrupts your image.
            byte[] bodyBytes = bodySection.getBytes(StandardCharsets.ISO_8859_1);

            System.out.println("[CH] [PARSER] Converted String to Bytes. Size: " + bodyBytes.length);

            request.setBody(bodyBytes);
        } else {
            request.setBody(new byte[0]);
        }

        return request;
    } 
    /**
     * Parse: GET /path HTTP/1.1
     */
    private static void parseRequestLine(String line, HttpRequest request) {
        String[] parts = line.split(" ");

        if (parts.length < 2) {
            return; // Invalid request line
        }

        request.setMethod(parts[0]);
        request.setUri(parts[1]);

        // Set path (simple assignment, no query parsing as it's not a mandatory
        // requirement)
        request.setPath(parts[1]);

        if (parts.length > 2) {
            request.setHttpVersion(parts[2]);
        }

        // logger.debug("Parsed Request Line: Method=" + request.getMethod() + " Path="
        // + request.getPath());
    }

    /**
     * Parse Headers: Host, Content-Length, Cookie, etc.
     */
    private static void parseHeaderLine(String line, HttpRequest request) {
        int colonIndex = line.indexOf(':');

        if (colonIndex <= 0)
            return;

        String name = line.substring(0, colonIndex).trim();
        String value = line.substring(colonIndex + 1).trim();

        // Store generic header
        request.setHeaders(name, value);
        // logger.debug("Header added: " + name + "=" + value);

        // Handle Cookies (Mandatory Requirement)
        if (name.equalsIgnoreCase("Cookie")) {
            parseCookieHeader(value, request);
        }
    }

    /**
     * Parse Cookie: sessionId=abc123; userId=42
     */
    private static void parseCookieHeader(String cookieValue, HttpRequest request) {
        if (cookieValue == null || cookieValue.isEmpty())
            return;

        String[] cookies = cookieValue.split(";");

        for (String cookie : cookies) {
            int equalIndex = cookie.indexOf('=');

            if (equalIndex > 0) {
                String name = cookie.substring(0, equalIndex).trim();
                String value = cookie.substring(equalIndex + 1).trim();

                request.addCookie(name, value);
                // logger.debug("Cookie parsed: " + name + "=" + value);
            }
        }
    }
}