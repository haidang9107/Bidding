package org.example.server.network;

import org.example.util.Config;
import org.example.util.FileLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Non-blocking Socket Server using Java NIO.
 */
public class SocketServer {
    private final int cmdPort;
    private final int notifyPort;
    private Selector selector;
    private ServerSocketChannel cmdChannel;
    private ServerSocketChannel notifyChannel;
    private volatile boolean running = true;

    private final Map<SocketChannel, StringBuilder> clientBuffers = new ConcurrentHashMap<>();

    public SocketServer() {
        this.cmdPort = Config.getInt("SERVER_PORT");
        this.notifyPort = Config.getInt("NOTIFY_PORT");
    }

    public void run(String... args) {
        try {
            selector = Selector.open();

            cmdChannel = ServerSocketChannel.open();
            cmdChannel.bind(new InetSocketAddress(cmdPort));
            cmdChannel.configureBlocking(false);
            cmdChannel.register(selector, SelectionKey.OP_ACCEPT, "Command");

            notifyChannel = ServerSocketChannel.open();
            notifyChannel.bind(new InetSocketAddress(notifyPort));
            notifyChannel.configureBlocking(false);
            notifyChannel.register(selector, SelectionKey.OP_ACCEPT, "Notification");

            FileLogger.info("NIO Server started. CMD Port: " + cmdPort + ", Notify Port: " + notifyPort);

            while (running) {
                if (selector.select() == 0) continue;

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            FileLogger.error("NIO Server Critical Failure", e);
        } finally {
            stop();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        String type = (String) key.attachment();
        clientChannel.register(selector, SelectionKey.OP_READ, type);
        clientBuffers.put(clientChannel, new StringBuilder());

        FileLogger.info("New " + type + " connection from: " + clientChannel.getRemoteAddress());
        
        if ("Notification".equals(type)) {
            Broadcaster.addClient(new NotificationHandler(clientChannel));
        }
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        StringBuilder buffer = clientBuffers.get(clientChannel);
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        try {
            int bytesRead = clientChannel.read(readBuffer);
            if (bytesRead == -1) {
                closeConnection(clientChannel);
                return;
            }

            readBuffer.flip();
            String data = StandardCharsets.UTF_8.decode(readBuffer).toString();
            buffer.append(data);

            int newlineIndex;
            while ((newlineIndex = buffer.indexOf("\n")) != -1) {
                String message = buffer.substring(0, newlineIndex).trim();
                buffer.delete(0, newlineIndex + 1);

                if (!message.isEmpty()) {
                    String type = (String) key.attachment();
                    if ("Command".equals(type)) {
                        new Thread(new CommandHandler(clientChannel, message)).start();
                    }
                }
            }
        } catch (IOException e) {
            FileLogger.error("Error reading from client: " + clientChannel, e);
            closeConnection(clientChannel);
        }
    }

    private void closeConnection(SocketChannel channel) {
        try {
            FileLogger.info("Closing connection: " + channel.getRemoteAddress());
            clientBuffers.remove(channel);
            channel.close();
        } catch (IOException e) {
            FileLogger.error("Error closing channel", e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (selector != null) selector.close();
            if (cmdChannel != null) cmdChannel.close();
            if (notifyChannel != null) notifyChannel.close();
        } catch (IOException e) {
            FileLogger.error("Error stopping server", e);
        }
    }
}
