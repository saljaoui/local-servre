package handlers;

import config.model.WebServerConfig.ServerBlock;
 import http.model.HttpResponse;
import http.model.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ErrorHandler {

    public HttpResponse handle(ServerBlock server,  HttpStatus status) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(status.code);
        response.setStatusMessage(status.message);

        String errorPagePath = null;
        if (server != null && server.getErrorPages() != null) {
            errorPagePath = server.getErrorPages().get(String.valueOf(status.code));
        }

        if (errorPagePath != null) {
            File file = new File(errorPagePath);
            if (file.exists() && !file.isDirectory()) {
                try {
                    byte[] content = Files.readAllBytes(file.toPath());
                    response.setBody(content);
                    response.addHeader("Content-Type", "text/html; charset=UTF-8");
                    response.addHeader("Content-Length", String.valueOf(content.length));
                    return response;
                } catch (IOException ignored) {
                }
            }
        }

        String fallbackBody = status.code + " " + status.message;
        response.setBody(fallbackBody.getBytes());
        response.addHeader("Content-Type", "text/plain; charset=UTF-8");
        response.addHeader("Content-Length", String.valueOf(fallbackBody.length()));
        return response;
    }

    public HttpResponse notFound() {
        return handle(null, http.model.HttpStatus.NOT_FOUND);
    }

    public HttpResponse notFound(ServerBlock server) {
        return handle(server, http.model.HttpStatus.NOT_FOUND);
    }

    public HttpResponse methodNotAllowed(ServerBlock server) {
        return handle(server, http.model.HttpStatus.METHOD_NOT_ALLOWED);
    }

}
