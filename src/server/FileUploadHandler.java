package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import util.SonicLogger;

public class FileUploadHandler {

    private static final SonicLogger logger = SonicLogger.getLogger(FileUploadHandler.class);

    private File tempFile;
    private FileOutputStream tempFileOutputStream;

    private long bytesWrittenToFile = 0;

    private byte[] boundaryBytes;
    private byte[] boundaryCarry = new byte[0];

    private boolean fileComplete = false;

    public void initialize(String boundary) throws IOException {
        this.boundaryBytes = ("\r\n--" + boundary).getBytes();
        createTempFile();
    }

    private void createTempFile() throws IOException {
        tempFile = File.createTempFile("uploads", ".tmp");
        tempFileOutputStream = new FileOutputStream(tempFile);

        File meta = new File(tempFile.getParent(), tempFile.getName() + ".meta");
        try (FileWriter w = new FileWriter(meta)) {
            w.write("upload_time=" + System.currentTimeMillis());
        }

        logger.info("Created temp file: " + tempFile.getAbsolutePath());
    }

    public void processData(byte[] data) throws IOException {
        if (tempFileOutputStream == null || fileComplete) return;

        byte[] combined = new byte[boundaryCarry.length + data.length];
        System.arraycopy(boundaryCarry, 0, combined, 0, boundaryCarry.length);
        System.arraycopy(data, 0, combined, boundaryCarry.length, data.length);

        int writeLimit = combined.length;
        int boundaryPos = findBoundary(combined, boundaryBytes);

        if (boundaryPos != -1) {
            writeLimit = boundaryPos;
            fileComplete = true;
        }

        if (writeLimit > 0) {
            tempFileOutputStream.write(combined, 0, writeLimit);
            bytesWrittenToFile += writeLimit;
        }

        int carryLen = Math.min(boundaryBytes.length, combined.length);
        boundaryCarry = Arrays.copyOfRange(
                combined,
                combined.length - carryLen,
                combined.length
        );

        if (fileComplete) {
            logger.info("File upload complete: " + bytesWrittenToFile + " bytes");
        }
    }

    private int findBoundary(byte[] data, byte[] boundary) {
        for (int i = 0; i <= data.length - boundary.length; i++) {
            boolean match = true;
            for (int j = 0; j < boundary.length; j++) {
                if (data[i + j] != boundary[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    public boolean isComplete() {
        return fileComplete;
    }

    public File getUploadedFile() {
        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
                tempFileOutputStream = null;
            }
        } catch (IOException ignored) {}
        return tempFile;
    }

    public void cleanup() {
        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
            }
        } catch (IOException ignored) {}

        if (!fileComplete && tempFile != null) {
            tempFile.delete();
        }
    }

    public void reset() {
        cleanup();
        tempFile = null;
        bytesWrittenToFile = 0;
        boundaryBytes = null;
        boundaryCarry = new byte[0];
        fileComplete = false;
    }
}
