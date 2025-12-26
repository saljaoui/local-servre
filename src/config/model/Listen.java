package config.model;

public final class Listen {
    private final String host;
    private final int port;
    private final boolean isDefault;

    public Listen(String host, int port, boolean isDefault) {
        this.host = host;
        this.port = port;
        this.isDefault = isDefault;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isDefault() { return isDefault; }

    @Override
    public String toString() {
        return "Listen{host='" + host + "', port=" + port + ", isDefault=" + isDefault + "}";
    }
}
