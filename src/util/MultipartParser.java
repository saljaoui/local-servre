package util;

import http.model.HttpRequest;
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
 

}