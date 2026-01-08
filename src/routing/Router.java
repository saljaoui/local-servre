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
import http.model.HttpStatus;
import routing.model.Route;
import util.SonicLogger;

public class Router {

    private static final SonicLogger logger = SonicLogger.getLogger(Router.class);

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
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        var method = request.getMethod();
        // System.out.println("Router.routeRequest()"+server.getRoutes()+ " " + request.getMethod());
        if (!route.isMethodAllowed(method)) {
            // System.err.println("her 0");
            return errorHandler.handle(server, HttpStatus.METHOD_NOT_ALLOWED);
        }
        if (request.getMethod().equals("POST") && route.getPath().equals("/")) {
            // System.out.println("Resved");
            return staticHandler.handle(request, server, route);
            // return new HttpResponse(HttpStatus.OK, "POST received".getBytes());
        }
        // Handle redirects first
        if (route.isRedirect()) {
            return redirectHandler.handle(route);
        }

        // Handle CGI requests
        if (route.isCgiEnabled()) {
            return cgiHandler.handle(request, server, route);
        }

        // Handle file uploads
        if (route.isUploadEnabled() && "POST".equalsIgnoreCase(method)) {
            logger.debug("Handling upload for route: " + route.getUpload().getFileField());
            return uploadHandler.handle(request, route, server);
        }

        // Handle DELETE requests
        // System.out.println("Router.routeRequest()"+method);
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

            // Exact match
            if (requestPath.equals(routePath)) {
                return route;
            }

            // Prefix match
            if (requestPath.startsWith(routePath)) {
                if (routePath.length() > longestMatch) {
                    bestMatch = route;
                    longestMatch = routePath.length();
                }
            }
        }
        return bestMatch;
    }
}
