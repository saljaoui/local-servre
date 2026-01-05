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
    private boolean foundFileStart = false;
    private boolean inFileContent = false;
    private boolean fileComplete = false;

    public void initialize(String boundary) throws IOException {
        this.boundaryBytes = ("\r\n--" + boundary).getBytes();
        createTempFile();
    }

    private void createTempFile() throws IOException {
        tempFile = File.createTempFile("uploads", ".tmp");
        tempFileOutputStream = new FileOutputStream(tempFile);
        
        File metadataFile = new File(tempFile.getParent(), tempFile.getName() + ".meta");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            writer.write("upload_time=" + System.currentTimeMillis() + "\n");
        }

        logger.debug("Created temp file: " + tempFile.getPath());
        logger.debug("Created metadata file: " + metadataFile.getPath());
    }

    public void processData(byte[] data) throws IOException {
        if (tempFileOutputStream == null || fileComplete) {
            return;
        }

        if (!foundFileStart) {
            processInitialData(data);
        } else if (inFileContent) {
            processContinuedData(data);
        }

        if (bytesWrittenToFile % (1024 * 1024) == 0 && bytesWrittenToFile > 0) {
            logger.info("Received " + (bytesWrittenToFile / 1024 / 1024) + "MB");
        }
    }

    private void processInitialData(byte[] data) throws IOException {
        String dataStr = new String(data);
        int contentStart = dataStr.indexOf("\r\n\r\n");

        if (contentStart != -1) {
            foundFileStart = true;
            inFileContent = true;

            byte[] fileContent = Arrays.copyOfRange(data, contentStart + 4, data.length);
            writeContent(fileContent);
        }
    }

    private void processContinuedData(byte[] data) throws IOException {
        writeContent(data);
    }

    private void writeContent(byte[] content) throws IOException {
        int boundaryPos = findBoundary(content, boundaryBytes);

        if (boundaryPos != -1) {
            tempFileOutputStream.write(content, 0, boundaryPos);
            bytesWrittenToFile += boundaryPos;
            inFileContent = false;
            fileComplete = true;
            logger.info("File upload complete: " + bytesWrittenToFile + " bytes");
        } else {
            tempFileOutputStream.write(content);
            bytesWrittenToFile += content.length;
        }
    }

    private int findBoundary(byte[] data, byte[] boundary) {
        if (data == null || boundary == null || data.length < boundary.length) {
            return -1;
        }

        for (int i = 0; i <= data.length - boundary.length; i++) {
            boolean match = true;
            for (int j = 0; j < boundary.length; j++) {
                if (data[i + j] != boundary[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    public File getUploadedFile() {
        if (tempFile == null) {
            return null;
        }

        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
                tempFileOutputStream = null;
            }
        } catch (IOException e) {
            logger.error("Error closing temp file output stream: " + e.getMessage(), e);
        }

        return tempFile;
    }

    public void cleanup() {
        try {
            if (tempFileOutputStream != null) {
                tempFileOutputStream.close();
                tempFileOutputStream = null;
            }

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                logger.debug("Deleted temp file: " + tempFile.getPath());
            }
        } catch (IOException e) {
            logger.error("Error cleaning up temp file: " + e.getMessage(), e);
        }
    }

    public boolean isComplete() {
        return fileComplete;
    }

    public long getBytesWritten() {
        return bytesWrittenToFile;
    }

    public void reset() {
        cleanup();
        tempFile = null;
        bytesWrittenToFile = 0;
        foundFileStart = false;
        inFileContent = false;
        fileComplete = false;
        boundaryBytes = null;
    }
}
