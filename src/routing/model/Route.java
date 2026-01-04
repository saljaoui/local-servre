package routing.model;

import java.util.List;

import handlers.model.Cgi;
import handlers.model.Upload;

public class Route {

    private String path;
    private List<String> methods;
    private String root;
    private String index;
    private boolean autoIndex;
    private Upload upload;
    private Cgi cgi;
    private Redirect redirect;

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public boolean isAutoIndex() {
        return autoIndex;
    }

    public void setAutoIndex(boolean autoIndex) {
        this.autoIndex = autoIndex;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public Cgi getCgi() {
        return cgi;
    }

    public void setCgi(Cgi cgi) {
        this.cgi = cgi;
    }

    public Redirect getRedirect() {
        return redirect;
    }

    public void setRedirect(Redirect redirect) {
        this.redirect = redirect;
    }

    // Utility methods
    public boolean isMethodAllowed(String method) {
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        return methods.stream().anyMatch(m -> m.equalsIgnoreCase(method));
    }

    public boolean isUploadEnabled() {
        return upload != null && upload.isEnabled();
    }

    public boolean isCgiEnabled() {
        return cgi != null && cgi.isEnabled();
    }

    public boolean isRedirect() {
        return redirect != null;
    }

    @Override
    public String toString() {
        return "Route{path='" + path + "', methods=" + methods
                + ", root='" + root + "'}";
    }
}
