package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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

    private final ErrorHandler errorHandler;
    private final SocketChannel channel;
    private final Router router;
    private final ServerBlock server;
    
    private final RequestReader requestReader;
    private final ResponseWriter responseWriter;
    private final FileUploadHandler fileUploadHandler;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private boolean isFileUpload = false;

    public ConnectionHandler(SocketChannel channel, ServerBlock server) {
        this.channel = channel;
        this.server = server;
        this.router = new Router();
        this.errorHandler = new ErrorHandler();
        this.requestReader = new RequestReader(channel);
        this.responseWriter = new ResponseWriter(channel);
        this.fileUploadHandler = new FileUploadHandler();
    }

    public ServerBlock getServer() {
        return server;
    }

    public boolean read(ServerBlock server) throws IOException {
        RequestReader.ReadResult result = requestReader.read(server);

        if (result.status == RequestReader.ReadResult.Status.CONNECTION_CLOSED) {
            this.close();
            return false;
        }

        if (result.status == RequestReader.ReadResult.Status.PAYLOAD_TOO_LARGE) {
            handlePayloadTooLarge();
            return false;
        }

        if (isMultipartFormData(result.data)) {
            return handleFileUpload(result, server);
        }

        if (result.isChunked) {
            return handleChunkedRequest(result, server);
        }

        return result.complete;
    }

    private void handlePayloadTooLarge() throws IOException {
        fileUploadHandler.cleanup();
        httpResponse = errorHandler.handle(server, HttpStatus.PAYLOAD_TOO_LARGE);
        ByteBuffer buffer = ByteBuffer.wrap(httpResponse.toString().getBytes());
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        this.close();
    }

    private boolean handleFileUpload(RequestReader.ReadResult result, ServerBlock server) throws IOException {
        if (!isFileUpload) {
            isFileUpload = true;
            String boundary = extractBoundary(new String(result.data));
            fileUploadHandler.initialize(boundary);

            byte[] bodyData = Arrays.copyOfRange(result.data, result.headerEndIndex + 4, result.data.length);
            if (bodyData.length > 0 && !result.isChunked) {
                fileUploadHandler.processData(bodyData);
            }
        }

        return fileUploadHandler.isComplete();
    }

    private boolean handleChunkedRequest(RequestReader.ReadResult result, ServerBlock server) throws IOException {
        ChunkedTransferDecoder.DecodeResult chunkResult = 
            ChunkedTransferDecoder.decode(result.data, result.headerEndIndex);

        if (!chunkResult.complete) {
            return false;
        }

        if (chunkResult.body.length > server.getClientMaxBodyBytes()) {
            handlePayloadTooLarge();
            return false;
        }

        if (isFileUpload && !fileUploadHandler.isComplete()) {
            fileUploadHandler.processData(chunkResult.body);
        }

        return isFileUpload ? fileUploadHandler.isComplete() : true;
    }

    public File getUploadedFile() {
        return isFileUpload ? fileUploadHandler.getUploadedFile() : null;
    }

    public void cleanupTempFile() {
        fileUploadHandler.cleanup();
    }

    public boolean isFileUpload() {
        return isFileUpload;
    }

    private boolean isMultipartFormData(byte[] data) {
        if (data == null) return false;
        String tmp = new String(data);
        return tmp.toLowerCase().contains("content-type: multipart/form-data");
    }

    private String extractBoundary(String headerString) {
        String[] lines = headerString.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-type: multipart/form-data")) {
                int boundaryIndex = line.indexOf("boundary=");
                if (boundaryIndex != -1) {
                    return line.substring(boundaryIndex + 9);
                }
            }
        }
        return null;
    }

    public void dispatchRequest() {
        try {
            httpRequest = ParseRequest.processRequest(requestReader.getRawBytes());
            httpRequest.setConnectionHandler(this);
            SessionManager.getInstance().attachSession(httpRequest);
            httpResponse = router.routeRequest(httpRequest, server);
            SessionManager.getInstance().appendSessionCookie(httpRequest, httpResponse);
        } catch (Exception ex) {
            logger.error("Error processing request", ex);
            httpResponse = errorHandler.handle(server, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseWriter.prepare(httpResponse);
    }

    public boolean write() throws IOException {
        boolean complete = responseWriter.write();
        if (complete) {
            resetForNextRequest();
        }
        return complete;
    }

    public void close() throws IOException {
        channel.close();
    }

    private void resetForNextRequest() {
        requestReader.reset();
        responseWriter.reset();
        fileUploadHandler.reset();
        isFileUpload = false;
        httpRequest = null;
        httpResponse = null;
    }
}