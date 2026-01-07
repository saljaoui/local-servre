package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import config.model.WebServerConfig.ServerBlock;
import handlers.ErrorHandler;
import http.ParseRequest;
import http.model.HttpRequest;
import http.model.HttpResponse;
import http.model.HttpStatus;
import routing.Router;
import session.SessionManager;
import util.SonicLogger;

public class ConnectionHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(ConnectionHandler.class);

    private final SocketChannel channel;
    private final Router router = new Router();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final ServerBlock server;

    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer;

    private ByteArrayOutputStream rawBytes = new ByteArrayOutputStream();
    private boolean headersParsed = false;
    private boolean isChunked = false;
    private boolean requestComplete = false;
    private int headerEndIndex = -1;
    private long expectedContentLength = 0;

    /* ===== Upload ===== */
    private boolean isFileUpload = false;
    private File tempFile;
    private FileOutputStream tempOut;
    private String boundary;
    private byte[] boundaryBytes;
    private byte[] boundaryCarry = new byte[0];
    private boolean multipartHeadersSkipped = false;
    private boolean fileComplete = false;
    private long bytesWritten = 0;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
    }

    /* ================= READ ================= */
    public ServerBlock getServer() {
        return server;
    }

    public boolean read(ServerBlock server) throws IOException {
        int n = channel.read(readBuffer);
        if (n == -1) {
            close();
            return false;
        }

        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        readBuffer.clear();

        rawBytes.write(data);

        if (!headersParsed) {
            String tmp = rawBytes.toString(StandardCharsets.ISO_8859_1);
            int idx = tmp.indexOf("\r\n\r\n");
            if (idx != -1) {
                headersParsed = true;
                headerEndIndex = idx;

                for (String line : tmp.substring(0, idx).split("\r\n")) {
                    String l = line.toLowerCase();
                    if (l.startsWith("content-length:")) {
                        expectedContentLength = Long.parseLong(line.split(":")[1].trim());
                    }
                    if (l.startsWith("transfer-encoding:") && l.contains("chunked")) {
                         isChunked = true;
                    }
                }

                if (tmp.toLowerCase().contains("multipart/form-data")) {
                    isFileUpload = true;
                    boundary = extractBoundary(tmp);
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    prepareTempFile();

                    byte[] body = Arrays.copyOfRange(rawBytes.toByteArray(), idx + 4, rawBytes.size());
                    if (body.length > 0 && !isChunked) {
                        processFileData(body);
                    }
                }
            }
            return false;
        }

        // if (isChunked) {
        //     ChunkResult res = decodeChunked(rawBytes.toByteArray(), headerEndIndex);
        //     if (!res.complete) {
        //         return false;
        //     }

        //     rawBytes.reset();
        //     rawBytes.write(normalizeHeaders(res.body.length).getBytes(StandardCharsets.ISO_8859_1));
        //     rawBytes.write(res.body);

        //     if (isFileUpload) {
        //         System.out.println("(1111)");
        //         processFileData(res.body);
        //     }
        //     requestComplete = !isFileUpload || fileComplete;
        //     return requestComplete;
        // }

        if (isFileUpload) { 

            processFileData(data);
            requestComplete = fileComplete;
            // System.err.println("-   -  "+requestComplete);
        } else {
            requestComplete = rawBytes.size() >= headerEndIndex + 4 + expectedContentLength;
        }

        return requestComplete;
    }

    /* ================= FILE UPLOAD ================= */
    private void prepareTempFile() throws IOException {
        tempFile = File.createTempFile("upload_", ".tmp");
        tempOut = new FileOutputStream(tempFile);
    }

    private void processFileData(byte[] data) throws IOException {
        if (fileComplete || tempOut == null) {
            return;
        }

        byte[] combined = new byte[boundaryCarry.length + data.length];
        // System.out.println(".(00000)"+Arrays.toString(combined));
        System.arraycopy(boundaryCarry, 0, combined, 0, boundaryCarry.length);
        System.arraycopy(data, 0, combined, boundaryCarry.length, data.length);

        int start = 0;

        if (!multipartHeadersSkipped) {
            int h = indexOf(combined, "\r\n\r\n".getBytes());
            if (h == -1) {
                boundaryCarry = combined;
                return;
            }
            start = h + 4;
            multipartHeadersSkipped = true;
        }
        int boundaryPos = indexOf(combined, boundaryBytes);
        int end = boundaryPos == -1 ? combined.length : boundaryPos;

        if (end > start) {
            tempOut.write(combined, start, end - start);
            bytesWritten += (end - start);
        }

        if (boundaryPos != -1) {
            fileComplete = true;
            tempOut.close();
            return;
        }

        int carry = Math.min(boundaryBytes.length, combined.length);
        boundaryCarry = Arrays.copyOfRange(combined, combined.length - carry, combined.length);
    }

    /* ================= DISPATCH ================= */
    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(rawBytes.toByteArray());
            httpRequest.setConnectionHandler(this);
            SessionManager.getInstance().attachSession(httpRequest);
            httpResponse = router.routeRequest(httpRequest, server);
            SessionManager.getInstance().appendSessionCookie(httpRequest, httpResponse);
            prepareResponse();
        } catch (Exception e) {
            logger.error("Request error", e);
            httpResponse = errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
            prepareResponse();
        }
    }

    /* ================= WRITE ================= */
    public boolean write() throws IOException {
        channel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            reset();
            return true;
        }
        return false;
    }

    private void prepareResponse() {
        byte[] body = httpResponse.getBody() == null ? new byte[0] : httpResponse.getBody();
        httpResponse.getHeaders().put("Content-Length", String.valueOf(body.length));
        httpResponse.getHeaders().put("Date",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
                .append(httpResponse.getStatusCode())
                .append(" ")
                .append(httpResponse.getStatusMessage())
                .append("\r\n");

        httpResponse.getHeaders().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");

        byte[] headers = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        writeBuffer = ByteBuffer.allocate(headers.length + body.length);
        writeBuffer.put(headers).put(body).flip();
    }

    private void reset() {
        rawBytes.reset();
        headersParsed = false;
        isChunked = false;
        isFileUpload = false;
        multipartHeadersSkipped = false;
        fileComplete = false;
        bytesWritten = 0;
        boundaryCarry = new byte[0];
        writeBuffer = null;
    }

    public File getUploadedFile() {
        return tempFile;
    }

    public boolean isFileUpload() {
        return isFileUpload;
    }

    public void close() throws IOException {
        channel.close();
    }

    /* ================= HELPERS ================= */
    private String extractBoundary(String headers) {
        for (String l : headers.split("\r\n")) {
            if (l.toLowerCase().contains("boundary=")) {
                return l.substring(l.indexOf("boundary=") + 9);
            }
        }
        return null;
    }

    private int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private String normalizeHeaders(long len) {
        return rawBytes.toString(StandardCharsets.ISO_8859_1)
                .replaceAll("(?i)transfer-encoding:.*\r\n", "")
                .replaceAll("(?i)content-length:.*\r\n", "")
                + "Content-Length: " + len + "\r\n\r\n";
    }

    private ChunkResult decodeChunked(byte[] data, int headerEnd) {
        int i = headerEnd + 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (true) {
            int crlf = indexOf(Arrays.copyOfRange(data, i, data.length), "\r\n".getBytes());
            if (crlf == -1) {
                return ChunkResult.incomplete();
            }

            int size = Integer.parseInt(new String(data, i, crlf,
                StandardCharsets.ISO_8859_1).trim(), 16);
            i += crlf + 2;

            if (size == 0) {
                return ChunkResult.complete(out.toByteArray());
            }
            if (i + size > data.length) {
                return ChunkResult.incomplete();
            }

            out.write(data, i, size);
            i += size + 2;
        }
    }

    private static class ChunkResult {

        boolean complete;
        byte[] body;

        static ChunkResult complete(byte[] b) {
            return new ChunkResult(true, b);
        }

        static ChunkResult incomplete() {
            return new ChunkResult(false, null);
        }

        private ChunkResult(boolean c, byte[] b) {
            complete = c;
            body = b;
        }
    }
}
