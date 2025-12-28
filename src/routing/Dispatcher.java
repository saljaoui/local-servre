package routing;

// import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

import http.HttpRequest;
import http.HttpResponse;
import http.IServlet;
import http.Servlet;

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
    public void dispatch(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        IServlet servlet = servletRegistry.get(path);
        if (servlet != null) {
            try {
                switch (request.getMethod()) {
                    case "GET" -> servlet.doGet( (http.HttpRequest) request, response);
                    case "POST" -> servlet.doPost((http.HttpRequest) request, response);
                    default -> {
                        response.setStatus(405, "Method Not Allowed");
                        response.setBody("Method Not Allowed");
                    }
                }
            } catch (Exception e) {
                response.setStatus(500, "Internal Server Error");
                response.setBody("Internal Server Error");
            }
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("Not Found");
        }
    }

}
