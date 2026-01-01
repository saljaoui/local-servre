package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import config.model.WebServerConfig.ServerBlock;

import routing.model.Route;
import util.SonicLogger;

public class StaticHandler {

    private static final SonicLogger logger =
            SonicLogger.getLogger(StaticHandler.class);

    public HttpResponse handle(HttpRequest request,
                               ServerBlock server,
                               Route route) {

        HttpResponse response = new HttpResponse();

        String root = route.getRoot();
        String path = request.getPath();

        if (path.equals("/")) {
            path = "/" + route.getIndex();
        }

        File file = new File(root + path);

        if (!file.exists() || file.isDirectory()) {
            logger.debug("Static 404: " + file.getPath());
            response.setStatusCode(404);
            return response;
        }

        try {
            byte[] content = Files.readAllBytes(file.toPath());
            response.setStatusCode(200);
            response.setBody(content);

        } catch (IOException e) {
            logger.error("Static read error: " + file.getPath(), e);
            response.setStatusCode(500);
        }

        return response;
    }
}

