package handlers;

import http.model.HttpResponse;
import http.model.HttpStatus;
import config.model.WebServerConfig.ServerBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ErrorHandler {

   public HttpResponse handle(ServerBlock server, HttpStatus status) {
    HttpResponse response = new HttpResponse();
    response.setStatusCode(status.code);
    response.setStatusMessage(status.message);

    String errorPagePath =
            server.getErrorPages().get(String.valueOf(status.code));

    if (errorPagePath != null) {
        File file = new File(errorPagePath);
        if (file.exists() && !file.isDirectory()) {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                response.setBody(content);
                response.addHeader("Content-Type", "text/html; charset=UTF-8");
                response.addHeader("Content-Length", String.valueOf(content.length));
                return response;
            } catch (IOException ignored) {}
        }
    }

    response.setBody(new byte[0]);
    return response;
}

}
