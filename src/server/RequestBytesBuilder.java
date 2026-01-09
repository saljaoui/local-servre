package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RequestBytesBuilder {

    public byte[] build(byte[] rawHeaderBytes, boolean removeChunked, long contentLengthToSet, byte[] bodyBytes)
            throws IOException {
        String headerStr = new String(rawHeaderBytes, StandardCharsets.ISO_8859_1);

        if (removeChunked) {
            headerStr = headerStr.replaceAll("(?im)^Transfer-Encoding:\\s*chunked\\s*\\r\\n", "");
            headerStr = headerStr.replaceAll("(?im)^Transfer-Encoding:\\s*.*chunked.*\\r\\n", "");
        }

        if (headerStr.matches("(?is).*\\r\\nContent-Length:\\s*\\d+\\s*\\r\\n.*")) {
            headerStr = headerStr.replaceFirst("(?is)\\r\\nContent-Length:\\s*\\d+\\s*\\r\\n",
                    "\r\nContent-Length: " + contentLengthToSet + "\r\n");
        } else {
            int idx = headerStr.indexOf("\r\n\r\n");
            if (idx >= 0) {
                headerStr = headerStr.substring(0, idx) + "\r\nContent-Length: " + contentLengthToSet
                        + headerStr.substring(idx);
            }
        }

        if (!headerStr.contains("\r\n\r\n")) {
            headerStr = headerStr + "\r\n\r\n";
        }

        byte[] modifiedHeaderBytes = headerStr.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream complete = new ByteArrayOutputStream();
        complete.write(modifiedHeaderBytes);

        if (bodyBytes != null && bodyBytes.length > 0) {
            complete.write(bodyBytes);
        }

        return complete.toByteArray();
    }
}
