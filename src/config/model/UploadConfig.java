package config.model;

public final class UploadConfig {
    private final boolean enabled;
    private final String dir;
    private final String fileField;

    public UploadConfig(boolean enabled, String dir, String fileField) {
        this.enabled = enabled;
        this.dir = dir;
        this.fileField = fileField;
    }

    public boolean isEnabled() { return enabled; }
    public String getDir() { return dir; }
    public String getFileField() { return fileField; }
}
