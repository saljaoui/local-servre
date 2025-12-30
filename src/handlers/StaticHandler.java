package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import config.model.WebServerConfig.ServerBlock;

import routing.model.Route;

public class StaticHandler {

    private final String wwwRoot = "www";

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        HttpResponse response = new HttpResponse();
        route.getPath();

        // Special route: /simo → send sample text
        if (route.getPath().equals("/simo")) {
            response.setStatusCode(200);
            response.setBody("Hello from /simo!".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        String path = "";
        if (route.getPath().equals("/")) {
            path = "/main/index.html";
        }
        File file = new File(wwwRoot + path);

        if (!file.exists() || file.isDirectory()) {
            response.setStatusCode(404);
            response.setBody("404 Not Found".getBytes());
            return response;
        }
        
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            response.setStatusCode(200);
            response.setBody(content);

            String contentType = Files.probeContentType(Path.of(file.getPath()));
            if (contentType == null)
                contentType = "text/plain";
            response.addHeader("Content-Type", contentType);

        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody("500 Internal Server Error".getBytes());
        }

        return response;
    }
}
