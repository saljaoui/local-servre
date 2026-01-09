package server;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class HttpHeaderReader {

    private final int maxHeaderSize;
    private final ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
    private boolean complete;
    private boolean tooLarge;
    private byte[] rawHeaderBytes;
    private byte[] initialBodyBytes;
    private byte[] pendingExtra = new byte[0];

    public HttpHeaderReader(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public void feed(byte[] data) {
        if (complete || tooLarge) {
            return;
        }

        int cap = maxHeaderSize + 4;
        int canTake = Math.min(data.length, Math.max(0, cap - headerBuffer.size()));
        if (canTake > 0) {
            headerBuffer.write(data, 0, canTake);
        }

        byte[] extra = (canTake < data.length)
                ? Arrays.copyOfRange(data, canTake, data.length)
                : new byte[0];

        byte[] headerData = headerBuffer.toByteArray();
        int headerEnd = findHeaderEnd(headerData);
        if (headerEnd == -1) {
            if (headerBuffer.size() >= cap) {
                tooLarge = true;
            }
            pendingExtra = extra;
            return;
        }

        int bodyStart = headerEnd + 4;
        rawHeaderBytes = Arrays.copyOfRange(headerData, 0, bodyStart);

        byte[] initialBodyFromHeaderBuf = (bodyStart < headerData.length)
                ? Arrays.copyOfRange(headerData, bodyStart, headerData.length)
                : new byte[0];

        initialBodyBytes = concat(initialBodyFromHeaderBuf, extra);
        pendingExtra = new byte[0];
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isTooLarge() {
        return tooLarge;
    }

    public byte[] getRawHeaderBytes() {
        return rawHeaderBytes;
    }

    public byte[] getInitialBodyBytes() {
        return initialBodyBytes == null ? new byte[0] : initialBodyBytes;
    }

    public byte[] drainPendingExtra() {
        byte[] extra = pendingExtra;
        pendingExtra = new byte[0];
        return extra;
    }

    private int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        if (a == null || a.length == 0) return (b == null ? new byte[0] : b);
        if (b == null || b.length == 0) return a;
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
