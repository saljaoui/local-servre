package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import config.model.WebServerConfig.ServerBlock;
import config.model.WebServerConfig.Redirect;
import routing.model.Route;

public class RedirectHandler {
    
    /**
     * Handle redirect requests.
     * Returns an HTTP response with the appropriate redirect status code.
     */
    public HttpResponse handle(HttpRequest request, Route route, ServerBlock server) {
        HttpResponse response = new HttpResponse();
        
        Redirect redirect = route.getRedirect();
        if (redirect == null) {
            response.setStatusCode(500);
            response.setBody("Internal Server Error: No redirect configured".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        response.setStatusCode(redirect.getStatus());
        response.addHeader("Location", redirect.getTo());
        
        // Add a simple body for browsers that don't follow redirects automatically
        String body = "<html><body><h1>" + redirect.getStatus() + " Redirect</h1>" +
                      "<p>Redirecting to: <a href=\"" + redirect.getTo() + "\">" + 
                      redirect.getTo() + "</a></p></body></html>";
        response.setBody(body.getBytes());
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", String.valueOf(body.length()));
        
        return response;
    }
}
