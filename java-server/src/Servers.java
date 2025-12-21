import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;

public class Servers {

    public void Start(final int port) {
        var clients = new HashSet<SocketChannel>();
        try (var serverSocketChannel = ServerSocketChannel.open();
                var selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            var buffer = ByteBuffer.allocate(1024);
            while (true) {
                if (selector.select() == 0) {
                    continue;
                }

                var selectedKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext();) {
                    var key = it.next();
                    it.remove();

                    if (key.isAcceptable()) {
                        if (key.channel() instanceof ServerSocketChannel channel) {
                            var client = channel.accept();
                            if (client == null) {
                                continue;
                            }
                            var socket = client.socket();
                            var info = socket.getInetAddress().getCanonicalHostName() + " " + socket.getPort();
                            System.out.println("Connected => " + info);
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                            clients.add(client);
                        } else {
                            throw new RuntimeException("Unknown Channel");
                        }
                    } else if (key.isReadable()) {
                        if (key.channel() instanceof SocketChannel client) {
                            try {
                                int bytesRead = client.read(buffer);
                                if (bytesRead == -1) {
                                    var socket = client.socket();
                                    var info = socket.getInetAddress().getCanonicalHostName() + " " + socket.getPort();
                                    System.out.println("Disconnected => " + info);
                                    client.close();
                                    clients.remove(client);
                                    buffer.clear();
                                    continue;
                                } else if (bytesRead == 0) {
                                    // nothing read, continue
                                    buffer.clear();
                                    continue;
                                }

                                buffer.flip();
                                var data = new String(buffer.array(), buffer.position(), buffer.remaining());
                                System.out.println("Received: " + data);

                                // Echo back the received bytes
                                while (buffer.hasRemaining()) {
                                    client.write(buffer);
                                }
                            } catch (IOException ex) {
                                try {
                                    client.close();
                                } catch (IOException ignore) {
                                }
                                clients.remove(client);
                            } finally {
                                buffer.clear();
                            }

                        } else {
                            throw new RuntimeException("Unknown Channel");

                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            for (var client : clients) {
                try {
                    client.close();
                } catch (Exception e) {
                    // best effort: log and continue
                    System.err.println("Failed to close client: " + e.getMessage());
                }
            }
        }
    }
}
