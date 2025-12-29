package handlers;

import http.model.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ErrorHandler {

    private final String errorPagesRoot = "error_pages"; // folder where 404.html is

    // 404 Not Found
    public HttpResponse notFound() {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(404);
        response.setStatusMessage("Not Found");

        // Path to 404.html
        File file = new File(errorPagesRoot + "/404.html");

        if (file.exists() && !file.isDirectory()) {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                response.setBody(content);

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "text/html; charset=UTF-8");
                headers.put("Content-Length", String.valueOf(content.length));
                response.setHeaders(headers);

            } catch (IOException e) {
                // If reading fails, just leave body empty
                response.setBody(new byte[0]);
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Length", "0");
                response.setHeaders(headers);
            }
        } else {
            // If file doesn't exist, leave body empty
            response.setBody(new byte[0]);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Length", "0");
            response.setHeaders(headers);
        }

        return response;
    }
}
