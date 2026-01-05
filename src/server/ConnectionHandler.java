package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
    private final Server.PortContext portContext;
    private ServerPortSelection serverSelection;
    
    private final RequestReader requestReader;
    private final ResponseWriter responseWriter;
    private final FileUploadHandler fileUploadHandler;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private boolean isFileUpload = false;
    private boolean pendingResponse = false;

    public ConnectionHandler(SocketChannel channel, Server.PortContext portContext) {
        this.channel = channel;
        this.portContext = portContext;
        this.serverSelection = new ServerPortSelection(portContext);
        this.router = new Router();
        this.errorHandler = new ErrorHandler();
        this.requestReader = new RequestReader(channel);
        this.responseWriter = new ResponseWriter(channel);
        this.fileUploadHandler = new FileUploadHandler();
    }

    public boolean read() throws IOException {
        RequestReader.ReadResult result = requestReader.read(serverSelection.activeServer);

        if (result.status == RequestReader.ReadResult.Status.CONNECTION_CLOSED) {
            this.close();
            return false;
        }

        if (result.status == RequestReader.ReadResult.Status.PAYLOAD_TOO_LARGE) {
            handlePayloadTooLarge();
            return false;
        }

        if (result.isChunked) {
            return handleChunkedRequest(result);
        }

        if (isMultipartFormData(result.data)) {
            return handleFileUpload(result);
        }

        return result.complete;
    }

    private void handlePayloadTooLarge() throws IOException {
        fileUploadHandler.cleanup();
        httpResponse = errorHandler.handle(serverSelection.activeServer, HttpStatus.PAYLOAD_TOO_LARGE);
        responseWriter.prepare(httpResponse);
        pendingResponse = true;
    }

    private boolean handleFileUpload(RequestReader.ReadResult result) throws IOException {
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

    private boolean handleChunkedRequest(RequestReader.ReadResult result) throws IOException {
        ChunkedTransferDecoder.DecodeResult chunkResult = 
            ChunkedTransferDecoder.decode(result.data, result.headerEndIndex);

        if (!chunkResult.complete) {
            return false;
        }

        if (chunkResult.body.length > serverSelection.activeServer.getClientMaxBodyBytes()) {
            handlePayloadTooLarge();
            return false;
        }

        // Initialize multipart upload state for chunked uploads
        if (!isFileUpload && isMultipartFormData(result.data)) {
            isFileUpload = true;
            String boundary = extractBoundary(new String(result.data, 0, result.headerEndIndex, StandardCharsets.ISO_8859_1));
            fileUploadHandler.initialize(boundary);
        }

        if (isFileUpload && !fileUploadHandler.isComplete()) {
            fileUploadHandler.processData(chunkResult.body);
        }

        normalizeChunkedRawRequest(result, chunkResult.body);
        return isFileUpload ? fileUploadHandler.isComplete() : true;
    }

    private void normalizeChunkedRawRequest(RequestReader.ReadResult result, byte[] decodedBody) throws IOException {
        String headers = new String(result.data, 0, result.headerEndIndex, StandardCharsets.ISO_8859_1);
        String normalizedHeaders = ChunkedTransferDecoder.normalizeHeaders(headers, decodedBody.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(normalizedHeaders.getBytes(StandardCharsets.ISO_8859_1));
        out.write(decodedBody);
        requestReader.replaceRawBytes(out.toByteArray());
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
            serverSelection.selectForHost(httpRequest.getHeader("Host"));
            if (serverSelection.activeServer == null && portContext != null) {
                serverSelection.activeServer = portContext.getDefaultServer();
            }
            SessionManager.getInstance().attachSession(httpRequest);
            httpResponse = router.routeRequest(httpRequest, serverSelection.activeServer);
            SessionManager.getInstance().appendSessionCookie(httpRequest, httpResponse);
        } catch (Exception ex) {
            logger.error("Error processing request", ex);
            httpResponse = errorHandler.handle(serverSelection.activeServer, HttpStatus.INTERNAL_SERVER_ERROR);
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
        EventLoop.removeTracking(channel);
        channel.close();
    }

    private void resetForNextRequest() {
        requestReader.reset();
        responseWriter.reset();
        fileUploadHandler.reset();
        isFileUpload = false;
        httpRequest = null;
        httpResponse = null;
        pendingResponse = false;
    }

    private static class ServerPortSelection {
        private final Server.PortContext portContext;
        private ServerBlock activeServer;

        ServerPortSelection(Server.PortContext portContext) {
            this.portContext = portContext;
            this.activeServer = portContext.getDefaultServer();
        }

        void selectForHost(String hostHeader) {
            if (portContext == null) {
                return;
            }
            ServerBlock selected = portContext.selectServer(hostHeader);
            if (selected != null) {
                activeServer = selected;
            } else {
                activeServer = portContext.getDefaultServer();
            }
        }
    }

    public boolean hasPendingResponse() {
        return pendingResponse;
    }
}
