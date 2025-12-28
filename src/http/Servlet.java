package http;

import java.io.IOException;

public class Servlet implements IServlet {

    @Override
    public void doGet(HttpRequest request, HttpResponse response) throws IOException {
        // Auto-generated method stub
        // String user = request.getParameter("name"); // ?name=Alice

        // response.setContentType("text/html");
        // response.write("<html><body>");
        // response.write("<h1>Welcome Home!</h1>");
        // if (user != null) {
        //     response.write("<p>Hello, " + user + "!</p>");
        // }
        // response.write("<p>This page is served by a Java Servlet.</p>");
        // response.write("</body></html>");
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

}
