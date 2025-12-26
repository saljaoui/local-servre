package config.model;

import java.util.*;

public final class RouteConfig {
    private final String path;
    private final List<String> methods;
    private final String root;
    private final String index;
    private final boolean autoIndex;
    private final UploadConfig upload;
    private final CgiConfig cgi;
    private final RedirectConfig redirect;

    public RouteConfig(String path, List<String> methods, String root, String index, boolean autoIndex,
                       UploadConfig upload, CgiConfig cgi, RedirectConfig redirect) {
        this.path = path;
        this.methods = methods;
        this.root = root;
        this.index = index;
        this.autoIndex = autoIndex;
        this.upload = upload;
        this.cgi = cgi;
        this.redirect = redirect;
    }

    public String getPath() { return path; }
    public List<String> getMethods() { return methods; }
    public String getRoot() { return root; }
    public String getIndex() { return index; }
    public boolean isAutoIndex() { return autoIndex; }
    public UploadConfig getUpload() { return upload; }
    public CgiConfig getCgi() { return cgi; }
    public RedirectConfig getRedirect() { return redirect; }
}
