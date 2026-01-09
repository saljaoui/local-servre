package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BodyReceiver {

    public enum Mode { FIXED_LENGTH, CHUNKED }

    private static final int MEMORY_THRESHOLD = 1_048_576; // 1MB
    private static final int MAX_TRAILER_SIZE = 16_384;    // defensive

    private final long clientMaxBodyBytes;

    private Mode mode;
    private long contentLength;
    private long bodyBytesRead;
    private boolean done;

    // Storage
    private ByteArrayOutputStream bodyBuffer;
    private FileOutputStream bodyFileStream;
    private File tempBodyFile;

    // Chunked decoding state
    private enum ChunkState { READING_SIZE, READING_DATA, READING_TRAILERS }

    private ChunkState chunkState = ChunkState.READING_SIZE;
    private long currentChunkSize = -1;
    private long currentChunkRead = 0;
    private int chunkCrlfToConsume = 0;

    // Used for chunk-size line AND trailers accumulation
    private final ByteArrayOutputStream chunkLineBuf = new ByteArrayOutputStream();

    public BodyReceiver(long clientMaxBodyBytes) {
        this.clientMaxBodyBytes = clientMaxBodyBytes;
    }

    // Start modes

    public void startFixedLength(long contentLength) throws IOException {
        this.mode = Mode.FIXED_LENGTH;
        this.contentLength = contentLength;
        this.bodyBytesRead = 0;
        this.done = false;

        resetChunkedState();
        resetStorage();

        if (contentLength <= MEMORY_THRESHOLD) {
            bodyBuffer = new ByteArrayOutputStream((int) Math.min(contentLength, Integer.MAX_VALUE));
        } else {
            tempBodyFile = createTempFile();
            bodyFileStream = new FileOutputStream(tempBodyFile);
        }
    }

    public void startChunked() {
        this.mode = Mode.CHUNKED;
        this.contentLength = 0;
        this.bodyBytesRead = 0;
        this.done = false;

        resetChunkedState();
        resetStorage();

        // start in memory; upgrade to file after threshold
        bodyBuffer = new ByteArrayOutputStream(Math.min(MEMORY_THRESHOLD, 16 * 1024));
    }

    // Feeding

    /**
     * @return number of bytes CONSUMED from {@code data}
     */
    public int feed(byte[] data) throws IOException {
        if (data == null || data.length == 0) return 0;
        if (done) return 0;

        if (mode == Mode.FIXED_LENGTH) return feedFixedLength(data);
        return feedChunked(data);
    }

    private int feedFixedLength(byte[] data) throws IOException {
        long remaining = contentLength - bodyBytesRead;
        int toConsume = (int) Math.min((long) data.length, remaining);

        if (toConsume > 0) {
            writeBodyBytes(data, 0, toConsume);
        }

        if (bodyBytesRead >= contentLength) {
            close();
            done = true;
        }

        return toConsume;
    }

    /**
     * Chunked parsing:
     * - Reads chunk-size line (hex) terminated by CRLF
     * - Reads exactly that many bytes of payload
     * - Consumes CRLF after payload
     * - Repeats until 0-size, then reads trailers until either:
     *     a) empty trailer: CRLF
     *     b) trailer headers end: CRLFCRLF
     */
    private int feedChunked(byte[] data) throws IOException {
        int idx = 0;

        while (idx < data.length) {
            switch (chunkState) {
                case READING_SIZE -> {
                    int nextIdx = readLineFromData(data, idx);
                    if (nextIdx == -1) {
                        // need more bytes for the size line
                        return idx;
                    }

                    // chunkLineBuf contains "... \r\n"
                    String line = chunkLineBuf.toString(StandardCharsets.ISO_8859_1);
                    if (line.length() >= 2) line = line.substring(0, line.length() - 2); // strip CRLF
                    chunkLineBuf.reset();
                    idx = nextIdx;

                    // strip extensions after ';'
                    int semi = line.indexOf(';');
                    String sizePart = (semi >= 0 ? line.substring(0, semi) : line).trim();

                    try {
                        currentChunkSize = Long.parseLong(sizePart, 16);
                        if (currentChunkSize < 0) throw new NumberFormatException("negative");
                    } catch (NumberFormatException ex) {
                        throw new IOException("Invalid chunk size: " + sizePart, ex);
                    }

                    currentChunkRead = 0;
                    chunkCrlfToConsume = 0;

                    if (currentChunkSize == 0) {
                        chunkLineBuf.reset();
                        chunkState = ChunkState.READING_TRAILERS;
                    } else {
                        chunkState = ChunkState.READING_DATA;
                    }
                }

                case READING_DATA -> {
                    // 1) consume payload
                    if (currentChunkRead < currentChunkSize) {
                        long remaining = currentChunkSize - currentChunkRead;
                        int toConsume = (int) Math.min((long) (data.length - idx), remaining);

                        if (toConsume > 0) {
                            writeBodyBytes(data, idx, toConsume);
                            idx += toConsume;
                            currentChunkRead += toConsume;
                        }

                        if (currentChunkRead < currentChunkSize) {
                            return idx; // need more payload bytes
                        }

                        // payload complete, now consume trailing CRLF
                        chunkCrlfToConsume = 2;
                    }

                    // 2) consume trailing CRLF (validate)
                    while (chunkCrlfToConsume > 0) {
                        if (idx >= data.length) return idx;

                        byte b = data[idx];
                        if (chunkCrlfToConsume == 2 && b != '\r') {
                            throw new IOException("Malformed chunk: missing CR after payload");
                        }
                        if (chunkCrlfToConsume == 1 && b != '\n') {
                            throw new IOException("Malformed chunk: missing LF after payload");
                        }

                        idx++;
                        chunkCrlfToConsume--;
                    }

                    // next chunk size
                    currentChunkSize = -1;
                    currentChunkRead = 0;
                    chunkState = ChunkState.READING_SIZE;
                }

                case READING_TRAILERS -> {
                    // accumulate trailers (might be empty)
                    if (idx < data.length) {
                        chunkLineBuf.write(data, idx, data.length - idx);
                        idx = data.length;
                    }

                    byte[] trailerBytes = chunkLineBuf.toByteArray();
                    if (trailerBytes.length > MAX_TRAILER_SIZE) {
                        throw new TrailerTooLargeException("Trailer headers too large");
                    }

                    boolean empty = isEmptyTrailer(trailerBytes); // "\r\n"
                    boolean hasEnd = findHeaderEnd(trailerBytes) != -1; // "\r\n\r\n"

                    if (!empty && !hasEnd) {
                        return idx; // need more trailer bytes
                    }

                    close();
                    done = true;
                    return idx;
                }
            }
        }

        return idx;
    }

    // Writing + upgrade logic

    private void writeBodyBytes(byte[] src, int off, int len) throws IOException {
        if (len <= 0) return;

        bodyBytesRead += len;
        if (bodyBytesRead > clientMaxBodyBytes) {
            throw new BodyTooLargeException("Payload too large");
        }

        if (tempBodyFile == null && bodyBytesRead > MEMORY_THRESHOLD) {
            upgradeMemoryToFile();
        }

        if (tempBodyFile != null) {
            if (bodyFileStream == null) bodyFileStream = new FileOutputStream(tempBodyFile, true);
            bodyFileStream.write(src, off, len);
        } else {
            if (bodyBuffer == null) bodyBuffer = new ByteArrayOutputStream();
            bodyBuffer.write(src, off, len);
        }
    }

    private void upgradeMemoryToFile() throws IOException {
        tempBodyFile = createTempFile();
        bodyFileStream = new FileOutputStream(tempBodyFile);

        if (bodyBuffer != null && bodyBuffer.size() > 0) {
            bodyFileStream.write(bodyBuffer.toByteArray());
            bodyBuffer = null;
        }
    }

    // Helpers

    private void resetChunkedState() {
        currentChunkSize = -1;
        currentChunkRead = 0;
        chunkCrlfToConsume = 0;
        chunkLineBuf.reset();
        chunkState = ChunkState.READING_SIZE;
    }

    private void resetStorage() {
        close();
        bodyBuffer = null;
        tempBodyFile = null;
    }

    /**
     * Reads bytes into chunkLineBuf until CRLF is complete.
     * @return index after the LF, or -1 if line not complete.
     */
    private int readLineFromData(byte[] data, int start) {
        int idx = start;
        while (idx < data.length) {
            byte b = data[idx++];
            chunkLineBuf.write(b);

            if (b == '\n' && chunkLineBuf.size() >= 2) {
                byte[] buf = chunkLineBuf.toByteArray();
                if (buf[buf.length - 2] == '\r') {
                    return idx;
                }
            }
        }
        return -1;
    }

    private int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private boolean isEmptyTrailer(byte[] trailerBytes) {
        return trailerBytes.length == 2 && trailerBytes[0] == '\r' && trailerBytes[1] == '\n';
    }

    private File createTempFile() throws IOException {
        File uploadDir = new File(System.getProperty("java.io.tmpdir"), "http_uploads");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        return File.createTempFile("body_", ".tmp", uploadDir);
    }

    // Public getters

    public boolean isDone() { return done; }

    public long getDecodedLength() { return bodyBytesRead; }

    /**
     * Only valid when NOT using file. If using file, caller should use getTempFile().
     */
    public byte[] getBodyBytes() {
        if (tempBodyFile != null) return new byte[0];
        return bodyBuffer != null ? bodyBuffer.toByteArray() : new byte[0];
    }

    public File getTempFile() { return tempBodyFile; }

    public boolean isUsingFile() { return tempBodyFile != null; }

    public Mode getMode() { return mode; }

    public void close() {
        if (bodyFileStream != null) {
            try { bodyFileStream.close(); } catch (IOException ignored) {}
            bodyFileStream = null;
        }
    }

    // Exceptions

    public static class BodyTooLargeException extends IOException {
        public BodyTooLargeException(String message) { super(message); }
    }

    public static class TrailerTooLargeException extends IOException {
        public TrailerTooLargeException(String message) { super(message); }
    }
}
