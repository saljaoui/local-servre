package routing;

import config.model.WebServerConfig.ServerBlock;
import handlers.CgiHandler;
import handlers.DeleteHandler;
import handlers.ErrorHandler;
import handlers.RedirectHandler;
import handlers.StaticHandler;
import handlers.UploadHandler;
import http.model.HttpRequest;
import http.model.HttpResponse;
 import routing.model.Route;

public class Router {

    private final StaticHandler staticHandler;
    private final CgiHandler cgiHandler;
    private final RedirectHandler redirectHandler;
    private final ErrorHandler errorHandler;
    private final DeleteHandler deleteHandler;
    private final UploadHandler uploadHandler;

    public Router() {
        this.staticHandler = new StaticHandler();
        this.cgiHandler = new CgiHandler();
        this.redirectHandler = new RedirectHandler();
        this.errorHandler = new ErrorHandler();
        this.deleteHandler = new DeleteHandler();
        this.uploadHandler = new UploadHandler();
    }

    public HttpResponse routeRequest(HttpRequest request, ServerBlock server) {
        Route route = routerMatch(request, server);
         if (route == null) {
            return errorHandler.notFound();
        }

        var method = request.getMethod(); 
       
        if (!route.isMethodAllowed(method) && !"/uploads".equals(route.getPath())) { 
            return errorHandler.methodNotAllowed(server);
        }
        // Handle redirects first
        if (route.isRedirect()) {
            return redirectHandler.handle(route);
        }

        // Handle CGI requests
        if (route.isCgiEnabled()) {
            return cgiHandler.handle(request, route);
        }

        // Handle file uploads
        // System.out.println("[DEBUG] Checking upload for route: " + route.getu);
        if (route.isUploadEnabled() && "POST".equalsIgnoreCase(method)) {
            System.out.println("[DEBUG] Handling upload for route: " + route.getUpload().getFileField());
            return uploadHandler.handle(request, route, server);
        }

        // Handle DELETE requests
        if ("DELETE".equalsIgnoreCase(method)) {
            return deleteHandler.handle(request, route, server);
        }
 
        return staticHandler.handle(request, server, route);
    }

    private Route routerMatch(HttpRequest request, ServerBlock server) {
    if (server.getRoutes() == null || server.getRoutes().isEmpty()) {
        return null;
    }

    String requestPath = request.getPath();
    Route bestMatch = null;
    int longestMatch = 0;

    for (Route route : server.getRoutes()) {
        String routePath = route.getPath();

        // A. Exact Match (e.g., "/")
        if (requestPath.equals(routePath)) {
            return route;
        }

        // B. Prefix Match (e.g., Route="/uploads" Request="/uploads/file.jpg")
        // We check if request starts with route path
        if (requestPath.startsWith(routePath)) {
            // Ensure route isn't just "/" and request is longer
            if (routePath.length() > longestMatch) {
                bestMatch = route;
                longestMatch = routePath.length();
            }
        }
    }
    return bestMatch;
}
}
