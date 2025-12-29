package servlet;

import java.io.IOException;

import http.HttpRequest;
import http.HttpResponse;

public interface IServlet {

    // Standard Servlet methods
    void doGet(HttpRequest request, HttpResponse response) throws IOException;

    void doPost(HttpRequest request, HttpResponse response) throws IOException;

    // The specific path this servlet handles (e.g., "/", "/upload")
    String getPath();

    void doPut(HttpRequest request, HttpResponse response) throws IOException;

    void doDelete(HttpRequest request, HttpResponse response) throws IOException;
}