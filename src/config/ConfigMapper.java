// src/config/ConfigMapper.java
package config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.model.AppConfig;
import config.model.CgiConfig;
import config.model.Listen;
import config.model.RedirectConfig;
import config.model.RouteConfig;
import config.model.ServerConfig;
import config.model.Timeouts;
import config.model.UploadConfig;

public final class ConfigMapper {
    private ConfigMapper() {}

    public static AppConfig toAppConfig(Object raw) {
        Map<String, Object> root = obj(raw, "root");

        Timeouts timeouts = mapTimeouts(objReq(root, "timeouts"));

        List<Object> serversRaw = arrReq(root, "servers");
        List<ServerConfig> servers = new ArrayList<>();
        for (Object s : serversRaw) servers.add(mapServer(obj(s, "server")));

        return new AppConfig(timeouts, servers);
    }

    private static Timeouts mapTimeouts(Map<String, Object> t) {
        long header = numReq(t, "headerMillis");
        long body = numReq(t, "bodyMillis");
        long keep = numReq(t, "keepAliveMillis");
        return new Timeouts(header, body, keep);
    }

    private static ServerConfig mapServer(Map<String, Object> s) {
        String name = strReq(s, "name");
        String root = strReq(s, "root");
        long maxBody = numReq(s, "clientMaxBodyBytes");

        List<Listen> listens = new ArrayList<>();
        for (Object l : arrReq(s, "listen")) {
            Map<String, Object> lo = obj(l, "listen item");
            String host = strReq(lo, "host");
            int port = (int) numReq(lo, "port");
            boolean def = boolOpt(lo, "default", false);
            listens.add(new Listen(host, port, def));
        }

        List<String> serverNames = new ArrayList<>();
        for (Object n : arrReq(s, "serverNames")) serverNames.add(str(n, "serverNames item"));

        Map<Integer, String> errorPages = new HashMap<>();
        Map<String, Object> ep = objOpt(s, "errorPages");
        if (ep != null) {
            for (Map.Entry<String, Object> e : ep.entrySet()) {
                int code = Integer.parseInt(e.getKey());
                errorPages.put(code, str(e.getValue(), "errorPages[" + e.getKey() + "]"));
            }
        }

        List<RouteConfig> routes = new ArrayList<>();
        for (Object r : arrReq(s, "routes")) routes.add(mapRoute(obj(r, "route")));

        return new ServerConfig(name, listens, serverNames, root, maxBody, errorPages, routes);
    }

    private static RouteConfig mapRoute(Map<String, Object> r) {
        String path = strReq(r, "path");

        List<String> methods = new ArrayList<>();
        for (Object m : arrReq(r, "methods")) methods.add(str(m, "methods item"));

        String root = strOpt(r, "root", null);
        String index = strOpt(r, "index", null);
        boolean autoIndex = boolOpt(r, "autoIndex", false);

        UploadConfig upload = null;
        Map<String, Object> up = objOpt(r, "upload");
        if (up != null) {
            boolean enabled = boolOpt(up, "enabled", false);
            String dir = strOpt(up, "dir", null);
            String fileField = strOpt(up, "fileField", "file");
            upload = new UploadConfig(enabled, dir, fileField);
        }

        CgiConfig cgi = null;
        Map<String, Object> cg = objOpt(r, "cgi");
        if (cg != null) {
            boolean enabled = boolOpt(cg, "enabled", false);
            String binDir = strOpt(cg, "binDir", null);

            Map<String, Object> be = objOpt(cg, "byExtension");
            Map<String, String> byExt = new HashMap<>();
            if (be != null) {
                for (Map.Entry<String, Object> e : be.entrySet()) {
                    byExt.put(e.getKey(), str(e.getValue(), "byExtension[" + e.getKey() + "]"));
                }
            }
            cgi = new CgiConfig(enabled, binDir, byExt);
        }

        RedirectConfig redirect = null;
        Map<String, Object> rd = objOpt(r, "redirect");
        if (rd != null) {
            int status = (int) numReq(rd, "status");
            String to = strReq(rd, "to");
            redirect = new RedirectConfig(status, to);
        }

        return new RouteConfig(path, methods, root, index, autoIndex, upload, cgi, redirect);
    }

    // ---- helpers ----
    private static Map<String, Object> objReq(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing object: " + k);
        return obj(v, k);
    }

    private static Map<String, Object> objOpt(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : obj(v, k);
    }

    private static Map<String, Object> obj(Object v, String where) {
        if (!(v instanceof Map)) throw new IllegalArgumentException(where + " must be object");
        return (Map<String, Object>) v;
    }

    private static List<Object> arrReq(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing array: " + k);
        if (!(v instanceof List)) throw new IllegalArgumentException(k + " must be array");
        return (List<Object>) v;
    }

    private static long numReq(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing number: " + k);

        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Double) return ((Double) v).longValue();

        throw new IllegalArgumentException(k + " must be number");
    }

    private static boolean boolOpt(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v == null) return def;
        if (!(v instanceof Boolean)) throw new IllegalArgumentException(k + " must be boolean");
        return (Boolean) v;
    }

    private static String strReq(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing string: " + k);
        return str(v, k);
    }

    private static String strOpt(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : str(v, k);
    }

    private static String str(Object v, String where) {
        if (!(v instanceof String)) throw new IllegalArgumentException(where + " must be string");
        return (String) v;
    }
}
