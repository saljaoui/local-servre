package http;

import java.io.IOException;

public class Servlet implements IServlet {

    @Override
    public void doGet(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType("text/html");
        response.write("<html><body><h1>Welcome to the Home Page</h1></body></html>");
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
