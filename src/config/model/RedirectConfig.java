package config.model;

public final class RedirectConfig {
    private final int status;
    private final String to;

    public RedirectConfig(int status, String to) {
        this.status = status;
        this.to = to;
    }

    public int getStatus() { return status; }
    public String getTo() { return to; }

    @Override
    public String toString() {
        return "RedirectConfig{status=" + status + ", to='" + to + "'}";
    }
}
