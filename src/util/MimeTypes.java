package util; 

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {

    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        // Images
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("webp", "image/webp");
        MIME_MAP.put("svg", "image/svg+xml");

        // Text
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("html", "text/html");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("json", "application/json");

        // Documents
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("xml", "application/xml");
    }

    public static String getMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream"; // Default binary

        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        return MIME_MAP.getOrDefault(extension, "application/octet-stream");
    }
}