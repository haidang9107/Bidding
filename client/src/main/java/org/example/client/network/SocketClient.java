package org.example.client.network;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.Config;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class managing NIO socket connections to the server.
 */
public class SocketClient {
    private static volatile SocketClient instance;
    private Selector selector;
    private SocketChannel cmdChannel;
    private SocketChannel notifyChannel;
    private volatile boolean connected = false;

    private final Map<SocketChannel, StringBuilder> channelBuffers = new ConcurrentHashMap<>();

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    /**
     * Connects to the server using NIO.
     */
    public void connect() throws IOException {
        if (connected) return;

        String host = Config.get("SERVER_HOST");
        int port = Config.getInt("SERVER_PORT");
        int notifyPort = Config.getInt("NOTIFY_PORT");

        try {
            selector = Selector.open();

            // Connect Command Channel
            cmdChannel = SocketChannel.open();
            cmdChannel.configureBlocking(false);
            cmdChannel.connect(new InetSocketAddress(host, port));

            // Connect Notification Channel
            notifyChannel = SocketChannel.open();
            notifyChannel.configureBlocking(false);
            notifyChannel.connect(new InetSocketAddress(host, notifyPort));

            // Wait for connections to finish
            while (!cmdChannel.finishConnect() || !notifyChannel.finishConnect()) {
                // In a real app, you might want a timeout here
            }

            cmdChannel.register(selector, SelectionKey.OP_READ, "Command");
            notifyChannel.register(selector, SelectionKey.OP_READ, "Notification");

            channelBuffers.put(cmdChannel, new StringBuilder());
            channelBuffers.put(notifyChannel, new StringBuilder());

            connected = true;
            FileLogger.info("Connected to NIO Server at " + host + " (Ports: " + port + ", " + notifyPort + ")");

            // Start background listener thread
            new Thread(this::listenLoop, "NIOClientListener").start();

        } catch (IOException e) {
            FileLogger.error("Failed to connect to NIO Server", e);
            disconnect();
            throw e;
        }
    }

    private void listenLoop() {
        try {
            while (connected) {
                if (selector.select(1000) == 0) continue;

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            if (connected) {
                FileLogger.error("NIO Client Selector Error", e);
                disconnect();
            }
        }
    }

    private void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder buffer = channelBuffers.get(channel);
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        try {
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) {
                disconnect();
                return;
            }

            readBuffer.flip();
            buffer.append(StandardCharsets.UTF_8.decode(readBuffer));

            int newlineIndex;
            while ((newlineIndex = buffer.indexOf("\n")) != -1) {
                String message = buffer.substring(0, newlineIndex).trim();
                buffer.delete(0, newlineIndex + 1);

                if (!message.isEmpty()) {
                    Response<?> response = JsonConverter.fromJson(message, Response.class);
                    handleResponse(response, (String) key.attachment());
                }
            }
        } catch (IOException e) {
            FileLogger.error("Error reading from server", e);
            disconnect();
        }
    }

    public void sendRequest(Request request) {
        if (!connected) {
            FileLogger.error("Cannot send request: Not connected");
            return;
        }
        try {
            String json = JsonConverter.toJson(request) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                cmdChannel.write(buffer);
            }
        } catch (IOException e) {
            FileLogger.error("Failed to send request", e);
            disconnect();
        }
    }

    private void handleResponse(Response<?> response, String source) {
        FileLogger.info("[" + source + "] Received: " + response.getMessage());
    }

    public void disconnect() {
        connected = false;
        try {
            if (selector != null) selector.close();
            if (cmdChannel != null) cmdChannel.close();
            if (notifyChannel != null) notifyChannel.close();
            FileLogger.info("Disconnected from NIO Server.");
        } catch (IOException e) {
            FileLogger.error("Error during disconnect", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
