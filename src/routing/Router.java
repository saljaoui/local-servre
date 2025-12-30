package routing;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import handlers.StaticHandler;
import handlers.RedirectHandler;
import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.model.Route;

public class Router {

    private final StaticHandler staticHandler;
    // private final CGIHandler cgiHandler;
    private final RedirectHandler redirectHandler;
    private final ErrorHandler errorHandler;

    public Router() {
        this.staticHandler = new StaticHandler();
        // this.cgiHandler = new CGIHandler();
        this.redirectHandler = new RedirectHandler();
        this.errorHandler = new ErrorHandler();
    }

    public HttpResponse routeRequest(HttpRequest request, ServerBlock server) {
        Route route = routerMatch(request, server);

        if (route == null) {
            return errorHandler.notFound();
        }

        if (!route.isMethodAllowed(request.getMethod())) {
            return errorHandler.methodNotAllowed(server);
        }

        if (route.isRedirect()) {
            // return redirectHandler.handle(request, route, server);
        }

        if (route.isCgiEnabled()) {
            // return cgiHandler.handle(request, route, server);
        }

        return staticHandler.handle(request, server, route);
    }

    private Route routerMatch(HttpRequest request, ServerBlock server) {
        if (server.getRoutes() == null || server.getRoutes().isEmpty()) {
            return null;
        }

        for (Route route : server.getRoutes()) {
            if (route.getPath() != null && route.getPath().equals(request.getPath())) {
                return route;
            }
        }
        return null;
    }
}
