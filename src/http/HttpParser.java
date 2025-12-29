package http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import http.model.HttpRequest;
import util.SonicLogger;

public class HttpParser {
    private static final SonicLogger logger = SonicLogger.getLogger(HttpParser.class);
    private static final String HEADER_SEPARATOR = "\r\n\r\n";

    public static HttpRequest processRequest(String requestData) throws Exception {
        logger.info("Processing request");

        HttpRequest request = new HttpRequest();
        String raw = requestData.toString();
        logger.debug("Raw request length=" + raw.length());
        logger.debug("Raw request preview: " + (raw.length() > 200 ? raw.substring(0, 200) + "..." : raw));
        // split header
        // find end header and start body
        int headerEndIndex = requestData.indexOf(HEADER_SEPARATOR);
        if (headerEndIndex == -1) {
            throw new Exception("Invalid HTTP request: no header separator");
        }

        String headerSection = raw.substring(0, headerEndIndex);
        String bodySection = raw.substring(headerEndIndex + 4);// The data part (might be empty)

        logger.debug("Header end index=" + headerEndIndex + " body length=" + bodySection.length());
        logger.debug("Header section preview: "
                + (headerSection.length() > 200 ? headerSection.substring(0, 200) + "..." : headerSection));

        String[] linesHeader = headerSection.split("\r\n");
        // Line 0 is always the Request Line (GET / HTTP/1.1)
        parseRequestLine(linesHeader[0], request);

        logger.debug("Parsed request line: method=" + request.getMethod() + " uri=" + request.getUri());

        // Lines 1 to End are Headers (Host, Cookie, etc.)
        for (int i = 1; i < linesHeader.length; i++) {
            logger.debug("Parsing header line: " + linesHeader[i]);
            parseHeaderLine(linesHeader[i], request);
        }
        if (!bodySection.isEmpty()) {
            request.setBody(bodySection.getBytes(StandardCharsets.UTF_8));
            logger.debug("Set body bytes length=" + request.getBody().length);
        }
        return request;
    }

    private static void parseHeaderLine(String line, HttpRequest request) {
        // Split "GET /path?var=val HTTP/1.1" into ["GET", "/path?var=val", "HTTP/1.1"]
        int colonIndex = line.indexOf(":");
        if (colonIndex <= 0)
            return;
        String name = line.substring(0, colonIndex);
        String value = line.substring(colonIndex + 1).trim();
        request.setHeaders(name, value);
        logger.debug("Header added: " + name + "=" + value);
        if (name.equalsIgnoreCase("Cookie")) {
            // here parsCookie
            logger.debug("Parsing Cookie header value: " + value);
            parseCookieHeader(value, request);
        }
    }

    private static void parseParameters(String queryOnly, HttpRequest request) {
        // Auto-generated method stub
        if (queryOnly == null || queryOnly.isEmpty())
            return;

        String[] pairs = queryOnly.split("&");
        for (String parm : pairs) {
            int equalIndex = parm.indexOf("=");
            if (equalIndex > 0) {
                String key = parm.substring(0, equalIndex);
                String value = parm.substring(equalIndex + 1);
                try {
                    key = URLDecoder.decode(key, "UTF-8");
                    value = URLDecoder.decode(value, "UTF-8");
                    request.addQueryParam(key, value);
                    logger.debug("Query param parsed: " + key + "=" + value);
                } catch (Exception e) {
                    logger.error("Failed to decode query parameter: " + parm, e);
                }
            }
        }
    }

    private static void parseRequestLine(String line, HttpRequest request) {
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            return;
        }
        request.setMethod(parts[0]);
        // 2 save URI (the path +query)
        String uri = parts[1];
        request.setUri(uri);
        logger.debug("Request line parts: method=" + parts[0] + " uri=" + uri + " protocol="
                + (parts.length > 2 ? parts[2] : ""));
        // 3. Check for Query String (The ?name=alice part)
        int questionMarkIndex = uri.indexOf("?");
        if (questionMarkIndex != -1) {
            String pathOnly = uri.substring(0, questionMarkIndex); // "/search"
            String queryOnly = uri.substring(questionMarkIndex + 1); // "q=hello"

            request.setPath(pathOnly);
            request.setQueryString(queryOnly);

            logger.debug("URI has query string: path=" + pathOnly + " query=" + queryOnly);

            // Call Step 4 to parse "q=hello"
            parseParameters(queryOnly, request);
        } else {
            // No query string: the whole URI is the path
            request.setPath(uri);
            request.setQueryString("");
            logger.debug("URI has no query string: path=" + uri);
        }
        // Set HTTP version if present
        if (parts.length > 2) {
            request.setHttpVersion(parts[2]);
        }
    }

    private static void parseCookieHeader(String cookieValue, HttpRequest request) {
        if (cookieValue == null || cookieValue.isEmpty())
            return;

        // Split "a=1; b=2" into ["a=1", "b=2"]
        String[] cookies = cookieValue.split(";");

        for (String cookie : cookies) {
            // Split "a=1" into key and value
            int equalIndex = cookie.indexOf('=');

            if (equalIndex > 0) {
                String name = cookie.substring(0, equalIndex).trim();
                String value = cookie.substring(equalIndex + 1).trim();

                // Store in a specific Cookie map
                request.addCookie(name, value);
                logger.debug("Cookie parsed: " + name + "=" + value);
            }
        }
    }
}
