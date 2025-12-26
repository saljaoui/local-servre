package config.model;

public final class Timeouts {
    private final long headerMillis, bodyMillis, keepAliveMillis;

    public Timeouts(long headerMillis, long bodyMillis, long keepAliveMillis) {
        this.headerMillis = headerMillis;
        this.bodyMillis = bodyMillis;
        this.keepAliveMillis = keepAliveMillis;
    }

    public long getHeaderMillis() { return headerMillis; }
    public long getBodyMillis() { return bodyMillis; }
    public long getKeepAliveMillis() { return keepAliveMillis; }
}
