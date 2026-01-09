package util;

import http.model.HttpRequest;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MultipartParser {

    public static byte[] extractFileContent(HttpRequest request) {
        byte[] body = request.getBody();
        String ct = request.getHeader("Content-Type");
        if (ct == null || !ct.contains("boundary="))
            return body;

        String boundary = ct.split("boundary=")[1];
        byte[] first = ("--" + boundary + "\r\n").getBytes(StandardCharsets.ISO_8859_1);
        byte[] next = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        int start = indexOf(body, first, 0);
        if (start == -1)
            return body;

        int hdrEnd = indexOf(body, "\r\n\r\n".getBytes(), start);
        int dataStart = hdrEnd + 4;
        int dataEnd = indexOf(body, next, dataStart);

        return Arrays.copyOfRange(body, dataStart, dataEnd);
    }

    public static boolean extractFileContentToFile(File multipartFile, String contentType, File destination)
            throws IOException {
        if (multipartFile == null || contentType == null || !contentType.contains("boundary=")) {
            return false;
        }
        String boundary = contentType.split("boundary=")[1];
        try (InputStream input = new BufferedInputStream(new FileInputStream(multipartFile));
                OutputStream output = new FileOutputStream(destination)) {
            return streamExtractFileContent(input, boundary, output);
        }
    }

    public static int indexOf(byte[] source, byte[] target, int fromIndex) {
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

    private static boolean streamExtractFileContent(InputStream input, String boundary, OutputStream output)
            throws IOException {
        String boundaryLine = "--" + boundary;
        String endBoundaryLine = boundaryLine + "--";

        String line = readLine(input);
        if (line == null || (!line.equals(boundaryLine) && !line.equals(endBoundaryLine))) {
            return false;
        }
        if (line.equals(endBoundaryLine)) {
            return false;
        }

        // Skip headers.
        while ((line = readLine(input)) != null) {
            if (line.isEmpty()) {
                break;
            }
        }

        byte[] boundaryMarker = ("\r\n" + boundaryLine).getBytes(StandardCharsets.ISO_8859_1);
        byte[] buffer = new byte[8192];
        byte[] carry = new byte[0];
        int n;

        while ((n = input.read(buffer)) != -1) {
            byte[] combined = new byte[carry.length + n];
            System.arraycopy(carry, 0, combined, 0, carry.length);
            System.arraycopy(buffer, 0, combined, carry.length, n);

            int idx = indexOf(combined, boundaryMarker, 0);
            if (idx != -1) {
                output.write(combined, 0, idx);
                return true;
            }

            int safeLen = Math.max(0, combined.length - (boundaryMarker.length - 1));
            if (safeLen > 0) {
                output.write(combined, 0, safeLen);
                carry = Arrays.copyOfRange(combined, safeLen, combined.length);
            } else {
                carry = combined;
            }
        }

        return true;
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int prev = -1;
        int curr;
        while ((curr = input.read()) != -1) {
            if (prev == '\r' && curr == '\n') {
                break;
            }
            if (prev != -1) {
                line.write(prev);
            }
            prev = curr;
        }
        if (prev != -1 && !(prev == '\r' && curr == '\n')) {
            line.write(prev);
        }
        if (curr == -1 && line.size() == 0) {
            return null;
        }
        return line.toString(StandardCharsets.ISO_8859_1);
    }

}
