package config.model;

import java.util.Map;

public final class CgiConfig {
    private final boolean enabled;
    private final String binDir;
    private final Map<String, String> byExtension;

    public CgiConfig(boolean enabled, String binDir, Map<String, String> byExtension) {
        this.enabled = enabled;
        this.binDir = binDir;
        this.byExtension = byExtension;
    }

    public boolean isEnabled() { return enabled; }
    public String getBinDir() { return binDir; }
    public Map<String, String> getByExtension() { return byExtension; }

    @Override
    public String toString() {
        return "CgiConfig{enabled=" + enabled + ", binDir='" + binDir + "', byExtension=" + byExtension + "}";
    }
}
