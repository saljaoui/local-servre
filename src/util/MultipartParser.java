package util;

import http.model.HttpRequest;
import java.util.Arrays;

public class MultipartParser {

    public static byte[] extractFileContent(HttpRequest request) {
        byte[] rawBody = request.getBody();
        if (rawBody == null || rawBody.length == 0) {
            return new byte[0];
        }

        try {
            String contentType = request.getHeader("Content-Type");
            if (contentType == null || !contentType.contains("boundary=")) {
                // Not multipart
                return rawBody;
            }

            String boundaryStr = contentType.split("boundary=")[1].trim();
            // Use UTF-8 for boundary to support various filenames
            byte[] boundaryBytes = ("\r\n--" + boundaryStr).getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // =================================================================
            // THE FIX: Search for the FIRST boundary AFTER the preamble
            // =================================================================
            
            // Start searching from index 1, not 0. Why?
            // Because the very first bytes might be the boundary start (e.g., "----WebKit...").
            // If we search from 0, we find the "Start" boundary immediately, which is wrong.
            // We want to find the first "End" boundary (which marks the start of the content).
            // Actually, the structure is: --Boundary (Start) -> Headers -> --Boundary (Start/Content).
            // We want to find the SECOND occurrence of the boundary to isolate the content.
            
            int firstBoundaryIndex = indexOf(rawBody, boundaryBytes, 0);
            if (firstBoundaryIndex == -1) return rawBody;

            // Now, we are likely at the start of the FIRST Part.
            // We need to find the header delimiter AFTER this first boundary.
            byte[] headerDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            
            // Find start of content (after \r\n\r\n)
            int contentStart = indexOf(rawBody, headerDelimiter, firstBoundaryIndex);
            if (contentStart == -1) {
                // No headers found? Assume content starts right after boundary?
                contentStart = firstBoundaryIndex + boundaryBytes.length;
            } else {
                contentStart += 4; // Skip \r\n\r\n
            }

            // Find the NEXT boundary (the end of the content)
            int contentEnd = indexOf(rawBody, boundaryBytes, contentStart);
            if (contentEnd == -1) {
                // Fallback: If we can't find the end, just take everything to the end
                contentEnd = rawBody.length;
            }

            // =================================================================
            // DEBUG OUTPUT
            // =================================================================
            System.out.println("[CH] [MULTIPART] First Boundary at: " + firstBoundaryIndex);
            System.out.println("[CH] [MULTIPART] Content Start: " + contentStart);
            System.out.println("[CH] [MULTIPART] Content End: " + contentEnd);
            System.out.println("[CH] [MULTIPART] Extracted Size: " + (contentEnd - contentStart));

            return Arrays.copyOfRange(rawBody, contentStart, contentEnd);

        } catch (Exception e) {
            e.printStackTrace();
            return rawBody;
        }
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        outer:
        for (int i = fromIndex; i < source.length - target.length + 1; i++) {
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