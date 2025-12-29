package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ConnectionHandler {
    
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer writeBuffer;
    private String request = "";
    
    public ConnectionHandler(SocketChannel channel) {
        this.channel = channel;
    }
    
    // Read data from client - returns true when complete
    public boolean read() throws IOException {
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }
        
        // Convert buffer to string
        readBuffer.flip();
        byte[] data = new byte[readBuffer.remaining()];
        readBuffer.get(data);
        request += new String(data);
        readBuffer.clear();
        
        // HTTP request ends with double newline
        return request.contains("\r\n\r\n");
    }
    
    // Process the HTTP request and prepare response
    public void processRequest() {
        System.out.println("Processing request...");
        
        // YOUR HTML GOES HERE - Customize this!
        String html = buildYourHtml();
        
        // Build HTTP response with your HTML
        String response = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: text/html; charset=UTF-8\r\n" +
                         "Content-Length: " + html.length() + "\r\n" +
                         "Connection: close\r\n" +
                         "\r\n" +
                         html;
        
        writeBuffer = ByteBuffer.wrap(response.getBytes());
    }
    
    // READ YOUR HTML FILE - This loads index.html
    private String buildYourHtml() {
        try {
            // Path to your HTML file - change this if needed!
            java.nio.file.Path filePath = java.nio.file.Paths.get("www/main/index.html");
            
            // Read all bytes from file
            byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);
            
            // Convert to String with UTF-8 encoding
            String html = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            System.out.println("✓ Successfully loaded: " + filePath);
            return html;
            
        } catch (IOException e) {
            // If file not found, show error
            System.err.println("✗ Error reading HTML file: " + e.getMessage());
            System.err.println("✗ Looking for: www/main/index.html");
            System.err.println("✗ Current directory: " + System.getProperty("user.dir"));
            
            return "<html><body>" +
                   "<h1>Error 404 - File Not Found</h1>" +
                   "<p>Could not load <code>www/main/index.html</code></p>" +
                   "<p>Make sure the file exists in the correct location.</p>" +
                   "</body></html>";
        }
    }
    
    // Write response to client - returns true when complete
    public boolean write() throws IOException {
        if (writeBuffer == null) {
            return true;
        }
        
        channel.write(writeBuffer);
        
        // Return true when all data is written
        return !writeBuffer.hasRemaining();
    }
    
    // Close the connection
    public void close() throws IOException {
        System.out.println("Closing connection: " + channel.getRemoteAddress());
        channel.close();
    }
}