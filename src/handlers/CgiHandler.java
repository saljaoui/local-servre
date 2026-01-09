package handlers;

import config.model.WebServerConfig.ServerBlock;
import handlers.model.Cgi;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import routing.model.Route;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class CgiHandler {
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        Cgi cfg = route != null ? route.getCgi() : null;
        if (cfg == null || !cfg.isEnabled()) {
            return error(server, HttpStatus.NOT_FOUND);
        }
        if (route == null || route.getPath() == null) {
            return error(server, HttpStatus.NOT_FOUND);
        }

        String scriptPath = extractScriptPath(request.getPath(), route.getPath());
        if (scriptPath.isEmpty()) {
            return error(server, HttpStatus.NOT_FOUND);
        }

        return runScript(request, scriptPath, cfg, server);
    }

    private String extractScriptPath(String requestPath, String routePath) {
        requestPath = requestPath == null ? "" : requestPath;
        routePath = routePath == null ? "" : routePath;
        
        String script = requestPath.startsWith(routePath) 
            ? requestPath.substring(routePath.length()) 
            : requestPath;
        
        return script.startsWith("/") ? script.substring(1) : script;
    }

    private HttpResponse runScript(HttpRequest request, String scriptPath, Cgi cfg, ServerBlock server) {
        int queryIdx = scriptPath.indexOf('?');
        String cleanPath = queryIdx >= 0 ? scriptPath.substring(0, queryIdx) : scriptPath;

        File binDir = new File(cfg.getBinDir());
        File script = new File(binDir, cleanPath);

        try {
            String scriptCanonical = script.getCanonicalPath();
            if (!scriptCanonical.startsWith(binDir.getCanonicalPath())) {
                return error(server, HttpStatus.NOT_FOUND);
            }
            script = new File(scriptCanonical);
        } catch (IOException e) {
            return error(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!script.exists() || script.isDirectory()) {
            return error(server, HttpStatus.NOT_FOUND);
        }

        String interpreter = cfg.getInterpreterForExtension(getExtension(script.getName()));
        if (interpreter == null || interpreter.isEmpty()) {
            return error(server, HttpStatus.NOT_FOUND);
        }

        return executeCgi(request, script, interpreter, server);
    }

    private HttpResponse executeCgi(HttpRequest request, File script, String interpreter, ServerBlock server) {
        ProcessBuilder pb = new ProcessBuilder(interpreter, script.getAbsolutePath());
        pb.directory(script.getParentFile()).redirectErrorStream(true);
        
        setupEnvironment(pb, request, script, server);

        try {
            Process p = pb.start();
            
            writeRequestBody(p.getOutputStream(), request);

            byte[] output = readAll(p.getInputStream());
            return p.waitFor() == 0 ? parseCgiOutput(output) : error(server, HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error(server, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            return error(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void setupEnvironment(ProcessBuilder pb, HttpRequest request, File script, ServerBlock server) {
        var env = pb.environment();
        
        env.put("REQUEST_METHOD", request.getMethod() != null ? request.getMethod() : "GET");
        env.put("QUERY_STRING", getQueryString(request));
        env.put("CONTENT_LENGTH", getContentLengthForCgi(request));
        
        String ct = getHeader(request, "Content-Type", "");
        if (!ct.isEmpty()) env.put("CONTENT_TYPE", ct);
        String cookie = getHeader(request, "Cookie", "");
        if (!cookie.isEmpty()) env.put("HTTP_COOKIE", cookie);
        
        env.put("PATH_INFO", script.getAbsolutePath());
        env.put("SERVER_PROTOCOL", request.getHttpVersion() != null && !request.getHttpVersion().isEmpty()
            ? request.getHttpVersion() : "HTTP/1.1");
        
        if (server != null && server.getListen() != null) {
            env.put("SERVER_PORT", String.valueOf(server.getListen().getPort()));
            if (server.getServerNames() != null && !server.getServerNames().isEmpty()) {
                env.put("SERVER_NAME", server.getServerNames().get(0));
            }
        }
    }

    private String getQueryString(HttpRequest request) {
        String qs = request.getQueryString();
        if (qs != null && !qs.isEmpty()) return qs;
        
        String path = request.getPath();
        if (path != null) {
            int idx = path.indexOf('?');
            if (idx >= 0) return path.substring(idx + 1);
        }
        return "";
    }

    private void writeRequestBody(OutputStream outputStream, HttpRequest request) throws IOException {
        byte[] body = request.getBody();
        if (body != null && body.length > 0) {
            try (OutputStream os = outputStream) {
                os.write(body);
            }
            return;
        }

        File uploaded = request.getUploadedFile();
        if (uploaded == null || !uploaded.exists()) {
            outputStream.close();
            return;
        }

        try (OutputStream os = outputStream; FileInputStream fis = new FileInputStream(uploaded)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
    }

    private String getContentLengthForCgi(HttpRequest request) {
        String header = getHeader(request, "Content-Length", "");
        if (header != null && !header.isEmpty() && !"0".equals(header)) {
            return header;
        }
        byte[] body = request.getBody();
        if (body != null && body.length > 0) {
            return String.valueOf(body.length);
        }
        File uploaded = request.getUploadedFile();
        if (uploaded != null && uploaded.exists()) {
            return String.valueOf(uploaded.length());
        }
        return header != null ? header : "";
    }

    private HttpResponse parseCgiOutput(byte[] rawBytes) {
        HttpResponse response = new HttpResponse();
        String raw = new String(rawBytes);

        int sep = raw.indexOf("\r\n\r\n");
        if (sep < 0) sep = raw.indexOf("\n\n");
        
        String headerPart = sep >= 0 ? raw.substring(0, sep) : "";
        byte[] body = (sep >= 0 ? raw.substring(sep + (raw.charAt(sep) == '\r' ? 4 : 2)) : raw).getBytes();

        int statusCode = 200;
        String statusMsg = "OK";
        boolean hasContentType = false;

        for (String line : headerPart.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();

            if ("Status".equalsIgnoreCase(name)) {
                String[] parts = value.split("\\s+", 2);
                try {
                    statusCode = Integer.parseInt(parts[0]);
                    if (parts.length > 1) statusMsg = parts[1];
                } catch (NumberFormatException e) {
                    statusCode = 500;
                    statusMsg = "Internal Server Error";
                }
            } else {
                if ("Content-Type".equalsIgnoreCase(name)) hasContentType = true;
                response.addHeader(name, value);
            }
        }

        HttpStatus status = HttpStatus.fromCode(statusCode);
        if (status != null) {
            response.setStatus(status);
        } else {
            response.setStatusCode(statusCode);
            response.setStatusMessage(statusMsg);
        }
        response.setBody(body);

        if (!hasContentType) {
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
        }
        if (!response.getHeaders().containsKey("Content-Length")) {
            response.addHeader("Content-Length", String.valueOf(body.length));
        }

        return response;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private String getHeader(HttpRequest request, String name, String def) {
        if (request.getHeaders() == null) return def;
        String v = request.getHeaders().get(name);
        if (v == null) v = request.getHeaders().get(name.toLowerCase());
        return v != null ? v : def;
    }

    private HttpResponse error(ServerBlock server, HttpStatus status) {
        return errorHandler.handle(server, status);
    }
}
