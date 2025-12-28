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

    public boolean dispatch(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        IServlet servlet = servletRegistry.get(path);
        // 2. Prefix Match (e.g., "/users" matches "/users/1")
        if (servlet == null) {
            for (String routePath : servletRegistry.keySet()) {
                if (path.startsWith(routePath)) {
                    servlet = servletRegistry.get(routePath);
                    break;
                }
            }
        }
         if (servlet!=null){
            try {
                if ("GET".equalsIgnoreCase(request.getMethod())){
                    servlet.doGet(request, response);
                }
                else if ("POST".equalsIgnoreCase(request.getMethod())){
                    servlet.doPost(request, response);
                }
                else if ("PUT".equalsIgnoreCase(request.getMethod())){
                    servlet.doPut(request, response);
                }
                else if ("DELETE".equalsIgnoreCase(request.getMethod())){
                    servlet.doDelete(request, response);
                }
                else {
                    response.setStatus(405);
                    response.setStatusMessage("Method Not Allowed");
                    response.write("405 Method Not Allowed");
                }
            } catch (Exception e) {
                //  handle exception
            }
         }

        return false; // No servlet found

    }

}
