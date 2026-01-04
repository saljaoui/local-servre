package handlers;

import config.model.WebServerConfig.ServerBlock;
import handlers.model.Cgi;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import routing.model.Route;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;


public class CgiHandler {
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HttpResponse handle(HttpRequest request, ServerBlock server, Route route) {
        Cgi cfg = route != null ? route.getCgi() : null;
        if (cfg == null || !cfg.isEnabled()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        String requestPath = request.getPath() == null ? "" : request.getPath();
        String routePath = route.getPath() == null ? "" : route.getPath();
        String scriptRelative = requestPath.startsWith(routePath)
                ? requestPath.substring(routePath.length())
                : requestPath;
        if (scriptRelative.startsWith("/")) {
            scriptRelative = scriptRelative.substring(1);
        }
        if (scriptRelative.isEmpty()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        return runScript(request, scriptRelative, cfg, server);
    }

    private HttpResponse runScript(HttpRequest request, String scriptPath, Cgi cfg, ServerBlock server) {
        String cleanScriptPath = scriptPath;
        int q = cleanScriptPath.indexOf('?');
        if (q >= 0) {
            cleanScriptPath = cleanScriptPath.substring(0, q);
        }

        File binDir = new File(cfg.getBinDir());
        File script = new File(binDir, cleanScriptPath);

        try {
            String binCanonical = binDir.getCanonicalPath();
            String scriptCanonical = script.getCanonicalPath();
            if (!scriptCanonical.startsWith(binCanonical)) {
                return errorHandler.handle(server, HttpStatus.NOT_FOUND);
            }
            script = new File(scriptCanonical);
        } catch (IOException e) {
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!script.exists() || script.isDirectory()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        String ext = extractExtension(script.getName());
        String interpreter = cfg.getInterpreterForExtension(ext);
        if (interpreter == null || interpreter.isEmpty()) {
            return errorHandler.handle(server, HttpStatus.NOT_FOUND);
        }

        ProcessBuilder pb = new ProcessBuilder(interpreter, script.getAbsolutePath());
        pb.directory(script.getParentFile()).redirectErrorStream(true);

        String method = request.getMethod() == null ? "GET" : request.getMethod();
        pb.environment().put("REQUEST_METHOD", method);
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            int idx = requestPathIndex(request);
            if (idx >= 0) {
                queryString = request.getPath().substring(idx + 1);
            } else {
                queryString = "";
            }
        }
        pb.environment().put("QUERY_STRING", queryString);
        pb.environment().put("CONTENT_LENGTH",
                getHeader(request, "Content-Length",
                        request.getBody() != null ? String.valueOf(request.getBody().length) : ""));
        String ct = getHeader(request, "Content-Type", "");
        if (!ct.isEmpty()) pb.environment().put("CONTENT_TYPE", ct);
        pb.environment().put("PATH_INFO", script.getAbsolutePath());
        pb.environment().put("SERVER_PROTOCOL",
                request.getHttpVersion() != null && !request.getHttpVersion().isEmpty()
                        ? request.getHttpVersion()
                        : "HTTP/1.1");
        if (server != null && server.getListen() != null) {
            pb.environment().put("SERVER_PORT", String.valueOf(server.getListen().getPort()));
            if (server.getServerNames() != null && !server.getServerNames().isEmpty()) {
                pb.environment().put("SERVER_NAME", server.getServerNames().get(0));
            }
        }

        try {
            Process p = pb.start();
            try (OutputStream os = p.getOutputStream()) {
                if (request.getBody() != null && request.getBody().length > 0) {
                    os.write(request.getBody());
                }
            }

            byte[] out = readAll(p.getInputStream());
            if (p.waitFor() != 0) {
                return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return parseCgiOutput(out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            return errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpResponse parseCgiOutput(byte[] rawBytes) {
        HttpResponse response = new HttpResponse();
        String raw = new String(rawBytes);

        int sep = raw.indexOf("\r\n\r\n");
        int delim = 4;
        if (sep < 0) {
            sep = raw.indexOf("\n\n");
            delim = 2;
        }

        String headerPart = sep >= 0 ? raw.substring(0, sep) : "";
        String bodyPart = sep >= 0 ? raw.substring(sep + delim) : raw;

        int statusCode = 200;
        String statusMsg = "OK";
        boolean hasContentType = false;

        if (!headerPart.isEmpty()) {
            for (String line : headerPart.split("\\r?\\n")) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();

                if ("Status".equalsIgnoreCase(name)) {
                    try {
                        String[] parts = value.split("\\s+", 2);
                        statusCode = Integer.parseInt(parts[0]);
                        if (parts.length > 1) statusMsg = parts[1];
                    } catch (NumberFormatException ignored) {
                        statusCode = 500;
                        statusMsg = "Internal Server Error";
                    }
                    continue;
                }
                if ("Content-Type".equalsIgnoreCase(name)) {
                    hasContentType = true;
                }
                response.addHeader(name, value);
            }
        }

        byte[] body = bodyPart.getBytes();
        response.setStatusCode(statusCode);
        response.setStatusMessage(statusMsg);
        response.setBody(body);

        if (!hasContentType) {
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
        }
        if (!response.getHeaders().containsKey("Content-Length")) {
            response.addHeader("Content-Length", String.valueOf(body.length));
        }

        return response;
    }

    private String extractExtension(String filename) {
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
        try {
            if (request.getHeaders() == null) return def;
            String v = request.getHeaders().get(name);
            if (v == null) v = request.getHeaders().get(name.toLowerCase());
            return v != null ? v : def;
        } catch (Exception e) {
            return def;
        }
    }

    private int requestPathIndex(HttpRequest request) {
        String path = request.getPath();
        if (path == null) {
            return -1;
        }
        return path.indexOf('?');
    }
}
