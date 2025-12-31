package util;

import java.util.Arrays;

import http.model.HttpRequest;

public class MultipartParser {
    public static byte[] extractFileContent(HttpRequest request) {
        byte[] rawBody = request.getBody();

        // =================================================================
        // DEBUG 4: Check Input Size
        // =================================================================
        System.out.println("[CH] [MULTIPART] Input Body Size: " + (rawBody != null ? 0 : rawBody.length));

        if (rawBody == null || rawBody.length == 0)
            return new byte[0];

        try {
            String contentType = request.getHeader("Content-Type");
            if (contentType == null || !contentType.contains("boundary=")) {
                System.out.println("[CH] [MULTIPART] Not multipart, returning raw body");
                return rawBody;
            }

            String boundaryStr = contentType.split("boundary=")[1].trim();
            byte[] boundaryBytes = ("\r\n--" + boundaryStr).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

            System.out.println("[CH] [MULTIPART] Boundary: " + boundaryStr);

            int headerStart = indexOf(rawBody, boundaryBytes, 0);
            if (headerStart == -1)
                return rawBody;

            // Find end of headers (\r\n\r\n)
            byte[] headerDelimiter = new byte[] { '\r', '\n', '\r', '\n' };
            int contentStart = indexOf(rawBody, headerDelimiter, headerStart);

            if (contentStart == -1) {
                System.out.println("[CH] [MULTIPART] ERROR: Header Delimiter not found!");
                return rawBody;
            }

            int contentEnd = rawBody.length; // Default to end
            int closingBoundaryIndex = indexOf(rawBody, boundaryBytes, contentStart + 4);

            if (closingBoundaryIndex > 0) {
                contentEnd = closingBoundaryIndex;
            }

            // =================================================================
            // DEBUG 5: Check Extraction Size
            // =================================================================
            int extractedSize = (contentEnd - (contentStart + 4));
            System.out.println("[CH] [MULTIPART] Extracted Content Size: " + extractedSize + " bytes");

            byte[] result = Arrays.copyOfRange(rawBody, contentStart + 4, contentEnd);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return rawBody;
        }
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        outer: for (int i = fromIndex; i < source.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
