package server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ChunkedTransferDecoder {

    public static DecodeResult decode(byte[] data, int headerEnd) {
        int idx = headerEnd + 4;
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        while (idx < data.length) {
            int lineEnd = indexOfCrlf(data, idx);
            if (lineEnd == -1) {
                return DecodeResult.incomplete();
            }

            String sizeLine = new String(data, idx, lineEnd - idx, StandardCharsets.ISO_8859_1).trim();
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException e) {
                return DecodeResult.incomplete();
            }

            idx = lineEnd + 2; // past CRLF
            
            if (chunkSize == 0) {
                if (idx + 2 <= data.length) {
                    return DecodeResult.complete(body.toByteArray());
                }
                return DecodeResult.incomplete();
            }

            if (idx + chunkSize > data.length) {
                return DecodeResult.incomplete();
            }

            body.write(data, idx, chunkSize);
            idx += chunkSize;

            if (idx + 2 > data.length) {
                return DecodeResult.incomplete();
            }
            idx += 2; // skip trailing CRLF
        }

        return DecodeResult.incomplete();
    }

    public static String normalizeHeaders(String headerSection, long contentLength) {
        StringBuilder sb = new StringBuilder();
        String[] lines = headerSection.split("\r\n");
        
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.startsWith("transfer-encoding:") || lower.startsWith("content-length:")) {
                continue;
            }
            sb.append(line).append("\r\n");
        }
        
        sb.append("Content-Length: ").append(contentLength).append("\r\n\r\n");
        return sb.toString();
    }

    private static int indexOfCrlf(byte[] data, int start) {
        for (int i = start; i < data.length - 1; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    public static class DecodeResult {
        public final boolean complete;
        public final byte[] body;

        private DecodeResult(boolean complete, byte[] body) {
            this.complete = complete;
            this.body = body;
        }

        public static DecodeResult complete(byte[] body) {
            return new DecodeResult(true, body);
        }

        public static DecodeResult incomplete() {
            return new DecodeResult(false, null);
        }
    }
}