package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import config.model.WebServerConfig.ServerBlock;

public class RequestReader {

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final ByteArrayOutputStream rawBytes = new ByteArrayOutputStream();

    private boolean headersParsed = false;
    private int headerEndIndex = -1;
    private long expectedContentLength = 0;
    private long totalBytesRead = 0;
    private boolean isChunked = false;

    public RequestReader(SocketChannel channel) {
        this.channel = channel;
    }

    public ReadResult read(ServerBlock server) throws IOException {
        int bytesRead = channel.read(readBuffer);

        if (bytesRead == -1) {
            return ReadResult.connectionClosed();
        }

        totalBytesRead += bytesRead;

        if (totalBytesRead > server.getClientMaxBodyBytes()) {
            return ReadResult.payloadTooLarge();
        }

        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();
        rawBytes.write(data);

        if (!headersParsed) {
            parseHeaders();
        }

        boolean complete = isRequestComplete();
        return ReadResult.success(rawBytes.toByteArray(), complete, isChunked, headerEndIndex);
    }

    private void parseHeaders() {
        String temp = new String(rawBytes.toByteArray());
        int idx = temp.indexOf("\r\n\r\n");

        if (idx != -1) {
            headersParsed = true;
            headerEndIndex = idx;

            for (String line : temp.substring(0, idx).split("\r\n")) {
                String lower = line.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    expectedContentLength = Long.parseLong(line.split(":")[1].trim());
                } else if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {
                    isChunked = true;
                    expectedContentLength = 0;
                }
            }
        }

    }

    private boolean isRequestComplete() {
        if (!headersParsed) {
            return false;
        }
        if (isChunked) {
            return false; // Chunked requests handled separately

        }
        return expectedContentLength == 0 || rawBytes.size() >= headerEndIndex + 4 + expectedContentLength;
    }

    public byte[] getRawBytes() {
        return rawBytes.toByteArray();
    }

    public void replaceRawBytes(byte[] data) {
        rawBytes.reset();
        rawBytes.write(data, 0, data.length);
        parseHeaders();
    }

    public void reset() {
        rawBytes.reset();
        headersParsed = false;
        headerEndIndex = -1;
        expectedContentLength = 0;
        totalBytesRead = 0;
        isChunked = false;
    }

    public static class ReadResult {

        public final byte[] data;
        public final boolean complete;
        public final boolean isChunked;
        public final int headerEndIndex;
        public final Status status;

        public enum Status {
            SUCCESS, CONNECTION_CLOSED, PAYLOAD_TOO_LARGE
        }

        private ReadResult(byte[] data, boolean complete, boolean isChunked, int headerEndIndex, Status status) {
            this.data = data;
            this.complete = complete;
            this.isChunked = isChunked;
            this.headerEndIndex = headerEndIndex;
            this.status = status;
        }

        static ReadResult success(byte[] data, boolean complete, boolean isChunked, int headerEndIndex) {
            return new ReadResult(data, complete, isChunked, headerEndIndex, Status.SUCCESS);
        }

        static ReadResult connectionClosed() {
            return new ReadResult(null, false, false, -1, Status.CONNECTION_CLOSED);
        }

        static ReadResult payloadTooLarge() {
            return new ReadResult(null, false, false, -1, Status.PAYLOAD_TOO_LARGE);
        }
    }
}
