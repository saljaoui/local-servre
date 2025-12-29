package routing;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import handlers.StaticHandler;
import http.model.HttpRequest;
import http.model.HttpResponse;

// Minimal Router for audit
public class Router {

    private StaticHandler staticHandler;
    // private CGIHandler cgiHandler;
    private ErrorHandler errorHandler;

    public Router() {
        this.staticHandler = new StaticHandler();
        // this.cgiHandler = new CGIHandler();
        this.errorHandler = new ErrorHandler();
    }

    public HttpResponse routeRequest(HttpRequest request, ServerBlock server) {
        Route route = routerMatch(request);

        if (route == null) {
            // No route matched → return default response to prevent NPE
            return errorHandler.notFound();
        }

        // 2. Dispatch to the correct handler
        switch (route.getType()) {
            case STATIC:
                return staticHandler.handle(request, route);
            case CGI:
                // return cgiHandler.handle(request, route);
            case REDIRECT:
                // return errorHandler.redirect(route.getRedirectUrl()); 
            default:
                // return errorHandler.internalError();
                return new HttpResponse();
        }
    }

    private Route routerMatch(HttpRequest request) {

        if (request.getPath().equals("/")) {

            return new Route(Route.Type.STATIC, request.getPath());

        } else if (request.getPath().equals("/simo")) {

            return new Route(Route.Type.STATIC, request.getPath());


        } else if (request.getPath().endsWith(".py")) {
            return new Route(Route.Type.CGI, request.getPath());
        }

        // No match
        return null;
    }

    // Minimal Route class for internal use
    public static class Route {
        enum Type { STATIC, CGI, REDIRECT }
        private Type type;
        private String path;
        private String redirectUrl;

        public Route(Type type, String path) {
            this.type = type;
            this.path = path;
        }

        public Type getType() { return type; }
        public String getPath() { return path; }
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    }
}
