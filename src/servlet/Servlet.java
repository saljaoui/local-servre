package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import http.TemplateEngine;
import http.model.HttpRequest;
import http.model.HttpResponse;

public class Servlet implements IServlet {

    @Override
    public void doGet(HttpRequest request, HttpResponse response) throws IOException {
        // Auto-generated method stub
        Map<String, String> data = new HashMap<>();
        data.put("server_name", "MyJavaNioServer");
        data.put("path", request.getPath());
        
        // Get query param ?name=Alice
        // String user = request.param("name");
        // data.put("user", (user != null) ? user : "Guest");


        // 2. Parse Template (try known workspace location first)
        String htmlContent = TemplateEngine.render("www/main/index.html", data);

        // 3. Send Response
        // response.setContentType("text/html; charset=utf-8");
        // response.write(htmlContent);
    }

    @Override
    public void doPost(HttpRequest request, HttpResponse response) throws IOException {
        // Auto-generated method stub
        // response.write("POST not supported on home page");
    }

    @Override
    public String getPath() {
        // Auto-generated method stub
        return "/";
    }

    @Override
    public void doPut(HttpRequest request, HttpResponse response) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doPut'");
    }

    @Override
    public void doDelete(HttpRequest request, HttpResponse response) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doDelete'");
    }

}
