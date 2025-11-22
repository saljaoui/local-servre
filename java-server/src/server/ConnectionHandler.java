package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import utils.Logger;

public class ConnectionHandler {
    private static final String TAG = "Connection";
    private static final int BUFFER_SIZE = 8192;
    
    private final SocketChannel channel;
    private final SelectionKey key;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final long createdAt;
    private long lastActivity;
    
    // Connection state
    private State state;
    private StringBuilder requestData;
    private byte[] responseData;
    private int responsePosition;
    
    public enum State {
        READING_REQUEST,
        PROCESSING,
        WRITING_RESPONSE,
        CLOSED
    }
    
    public ConnectionHandler(SocketChannel channel, SelectionKey key) {
        this.channel = channel;
        this.key = key;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
        this.state = State.READING_REQUEST;
        this.requestData = new StringBuilder();
        
        Logger.debug(TAG, "New connection from: " + getRemoteAddress());
    }
    
    public void read() throws IOException {
        updateActivity();
        readBuffer.clear();
        
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            // Client closed connection
            close();
            return;
        }
        
        if (bytesRead > 0) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            requestData.append(new String(data));
            
            Logger.debug(TAG, "Read " + bytesRead + " bytes from " + getRemoteAddress());
            
            // Check if request is complete (ends with \r\n\r\n for headers)
            if (isRequestComplete()) {
                state = State.PROCESSING;
                processRequest();
            }
        }
    }
    
    public void write() throws IOException {
        updateActivity();
        
        if (responseData == null) {
            return;
        }
        
        writeBuffer.clear();
        int remaining = responseData.length - responsePosition;
        int toWrite = Math.min(remaining, writeBuffer.capacity());
        writeBuffer.put(responseData, responsePosition, toWrite);
        writeBuffer.flip();
        
        int bytesWritten = channel.write(writeBuffer);
        responsePosition += bytesWritten;
        
        Logger.debug(TAG, "Wrote " + bytesWritten + " bytes to " + getRemoteAddress());
        
        if (responsePosition >= responseData.length) {
            // Response complete
            Logger.debug(TAG, "Response complete for " + getRemoteAddress());
            close();
        }
    }
    
    private boolean isRequestComplete() {
        String data = requestData.toString();
        // Simple check: headers end with \r\n\r\n
        // TODO: Handle body for POST requests (Content-Length)
        return data.contains("\r\n\r\n");
    }
    
    private void processRequest() {
        Logger.info(TAG, "Processing request from " + getRemoteAddress());
        Logger.debug(TAG, "Request:\n" + requestData.toString().split("\r\n\r\n")[0]);
        
        // TODO: Parse request and route to handlers
        // For now, send a simple response
        String body = "<html><body><h1>Hello from LocalServer!</h1><p>Server is running.</p></body></html>";
        String response = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: text/html\r\n" +
                         "Content-Length: " + body.length() + "\r\n" +
                         "Connection: close\r\n" +
                         "\r\n" +
                         body;
        
        responseData = response.getBytes();
        responsePosition = 0;
        state = State.WRITING_RESPONSE;
        
        // Change interest to write
        key.interestOps(SelectionKey.OP_WRITE);
    }
    
    public void close() {
        state = State.CLOSED;
        try {
            Logger.debug(TAG, "Closing connection: " + getRemoteAddress());
            channel.close();
            key.cancel();
        } catch (IOException e) {
            Logger.error(TAG, "Error closing connection", e);
        }
    }
    
    private void updateActivity() {
        lastActivity = System.currentTimeMillis();
    }
    
    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastActivity > timeoutMs;
    }
    
    public State getState() {
        return state;
    }
    
    public String getRemoteAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
    
    public long getConnectionDuration() {
        return System.currentTimeMillis() - createdAt;
    }
}