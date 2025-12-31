package handlers;

import config.model.WebServerConfig.Redirect;
import http.model.HttpResponse;
import routing.model.Route;

public class RedirectHandler {

    public HttpResponse handle(Route route) {
        Redirect redirect = route.getRedirect();

        HttpResponse response = new HttpResponse();
        response.setStatusCode(redirect.getStatus());
        response.addHeader("Location", redirect.getTo());

        return response;
    }
}