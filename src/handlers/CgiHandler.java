package handlers;

import http.model.HttpRequest;
import http.model.HttpResponse;
import routing.model.Route;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class CgiHandler {
    private static final String FORM_PATH = "www/main/cgi.html";
    private static final String CGI_ROOT = "cgi-bin";
    private static final String DEFAULT_SCRIPT = "/script.py";

    public HttpResponse handle(HttpRequest request, Route route) {
        HttpResponse response = new HttpResponse();
        String path = request.getPath();
        String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase();

        if ("/cgi".equals(path) || "/cgi/".equals(path)) {
            if ("GET".equals(method)) return serveForm(response);
            if ("POST".equals(method)) return runScript(response, request, DEFAULT_SCRIPT);
            
            response.setStatusCode(405);
            response.setBody("Method Not Allowed".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        if (path.startsWith("/cgi/") && path.endsWith(".py")) {
            return runScript(response, request, path.substring(4));
        }

        response.setStatusCode(404);
        response.setBody("Not Found".getBytes());
        response.addHeader("Content-Type", "text/plain");
        return response;
    }

    private HttpResponse serveForm(HttpResponse response) {
        File f = new File(FORM_PATH);
        if (!f.exists() || f.isDirectory()) {
            response.setStatusCode(404);
            response.setBody("Not Found".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        try {
            response.setStatusCode(200);
            response.setBody(Files.readAllBytes(f.toPath()));
            response.addHeader("Content-Type", "text/html");
            return response;
        } catch (IOException e) {
            response.setStatusCode(500);
            response.setBody("Internal Server Error".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
    }

    private HttpResponse runScript(HttpResponse response, HttpRequest request, String scriptPath) {
        File script = new File(CGI_ROOT + scriptPath);
        if (!script.exists() || script.isDirectory()) {
            response.setStatusCode(404);
            response.setBody("CGI script not found".getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }

        ProcessBuilder pb = new ProcessBuilder("python3", script.getAbsolutePath());
        pb.directory(script.getParentFile()).redirectErrorStream(true);

        pb.environment().put("REQUEST_METHOD", request.getMethod() == null ? "GET" : request.getMethod());
        pb.environment().put("QUERY_STRING", extractQuery(request.getPath()));
        pb.environment().put("CONTENT_LENGTH", 
            getHeader(request, "Content-Length", request.getBody() != null ? String.valueOf(request.getBody().length) : ""));
        
        String ct = getHeader(request, "Content-Type", "");
        if (!ct.isEmpty()) pb.environment().put("CONTENT_TYPE", ct);

        try {
            Process p = pb.start();
            
            try (OutputStream os = p.getOutputStream()) {
                if (request.getBody() != null && request.getBody().length > 0) {
                    os.write(request.getBody());
                }
            }

            byte[] out = readAll(p.getInputStream());
            if (p.waitFor() != 0) {
                response.setStatusCode(500);
                response.setBody(out);
                response.addHeader("Content-Type", "text/plain");
                return response;
            }

            return parseCGI(response, out);

        } catch (IOException | InterruptedException e) {
            response.setStatusCode(500);
            response.setBody(("CGI error: " + e.getMessage()).getBytes());
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
    }

    private String extractQuery(String path) {
        if (path == null) return "";
        int idx = path.indexOf('?');
        return idx >= 0 && idx + 1 < path.length() ? path.substring(idx + 1) : "";
    }

    private HttpResponse parseCGI(HttpResponse response, byte[] out) {
        String raw = new String(out);
        int sep = raw.indexOf("\r\n\r\n");
        int len = sep >= 0 ? 4 : (sep = raw.indexOf("\n\n")) >= 0 ? 2 : -1;

        String cgiType = null;
        byte[] body = out;

        if (sep >= 0) {
            for (String line : raw.substring(0, sep).split("\\r?\\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && "Content-Type".equalsIgnoreCase(line.substring(0, colon).trim())) {
                    cgiType = line.substring(colon + 1).trim();
                    break;
                }
            }
            body = raw.substring(sep + len).getBytes();
        }

        response.setStatusCode(200);
        response.setBody(body);
        response.addHeader("Content-Type", cgiType != null ? cgiType : "text/html");
        return response;
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private String getHeader(HttpRequest request, String name, String def) {
        try {
            if (request.getHeaders() == null) return def;
            String v = request.getHeaders().get(name);
            if (v == null) v = request.getHeaders().get(name.toLowerCase());
            return v != null ? v : def;
        } catch (Exception e) {
            return def;
        }
    }
}