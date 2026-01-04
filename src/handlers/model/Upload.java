package handlers.model;

public class Upload {

    private boolean enabled;
    private String dir;
    private String fileField;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getFileField() {
        return fileField;
    }

    public void setFileField(String fileField) {
        this.fileField = fileField;
    }

    @Override
    public String toString() {
        return "Upload{enabled=" + enabled + ", dir='" + dir
                + "', fileField='" + fileField + "'}";
    }
}
