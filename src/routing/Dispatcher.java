package routing;

// import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

import http.HttpRequest;
import http.HttpResponse;
import http.IServlet;
import http.Servlet;
import server.ConnectionHandler;

public class Dispatcher {
    private final Map<String, IServlet> servletRegistry = new HashMap<>();

    public Dispatcher() {
        registerServlets();
    }

    private void registerServlets() {
        // servletRegistry.put(new HomeServlet, null)
        registerServlet(new Servlet());
    }

    private void registerServlet(IServlet servlet) {
        servletRegistry.put(servlet.getPath(), servlet);
    }

    public void dispatch(HttpRequest request, HttpResponse response, ConnectionHandler connection) {
        String path = request.getPath();
        IServlet servlet = servletRegistry.get(path);
        if (servlet != null) {
            try {
                System.out.println("[Dispatcher] Routing to Servlet: " + servlet.getClass().getSimpleName());

                switch (request.getMethod()) {
                    case "GET" -> servlet.doGet(request, response);
                    case "POST" -> servlet.doPost(request, response);
                    default -> {
                        response.setStatus(405, "Method Not Allowed");
                        response.setBody("Method Not Allowed");
                    }
                }
            } catch (Exception e) {
                response.setStatus(500, "Internal Server Error");
                response.setBody("Internal Server Error");
            }
        }

        // B. If NO Servlet matched, check your STANDARD ROUTES/CONFIG
        // This calls your existing StaticHandler, UploadHandler, etc.
        // Assuming you have a Router class that handles this logic:
        Router router = new Router(); // Or inject it via constructor
        boolean handled = router.routeRequest(request, response, connection);

        // C. If nothing handled it (Router returned false)
        if (!handled) {
            response.setStatus(404, " Not Found");

        }
    }

}
