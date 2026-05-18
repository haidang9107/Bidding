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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Singleton class managing unified NIO socket connection to the server.
 * Uses a single port for both commands and notifications.
 */
public class SocketClient {
    private static volatile SocketClient instance;
    private Selector selector;
    private SocketChannel clientChannel;
    private volatile boolean connected = false;

    private final Map<SocketChannel, StringBuilder> channelBuffers = new ConcurrentHashMap<>();
    private final List<Consumer<Response<?>>> observers = new ArrayList<>();

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

    public synchronized void addObserver(Consumer<Response<?>> observer) {
        observers.add(observer);
    }

    public synchronized void removeObserver(Consumer<Response<?>> observer) {
        observers.remove(observer);
    }

    private synchronized void notifyObservers(Response<?> response) {
        for (Consumer<Response<?>> observer : observers) {
            observer.accept(response);
        }
    }

    /**
     * Connects to the server using a single port.
     */
    public void connect() throws IOException {
        if (connected) return;

        String host = Config.get("SERVER_HOST");
        int port = Config.getInt("SERVER_PORT");

        try {
            selector = Selector.open();

            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress(host, port));

            while (!clientChannel.finishConnect()) {
                Thread.onSpinWait();
            }

            clientChannel.register(selector, SelectionKey.OP_READ);
            channelBuffers.put(clientChannel, new StringBuilder());

            connected = true;
            FileLogger.info("Connected to NIO Server at " + host + ":" + port);

            Thread listenerThread = new Thread(this::listenLoop, "NIOClientListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

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

                    if (key.isValid() && key.isReadable()) {
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
        ByteBuffer readBuffer = ByteBuffer.allocate(8192);

        try {
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) {
                FileLogger.warn("Server closed connection.");
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
                    if (response != null) {
                        notifyObservers(response);
                    }
                }
            }
        } catch (IOException e) {
            FileLogger.error("Error reading from server", e);
            disconnect();
        }
    }

    public synchronized void sendRequest(Request request) {
        if (!connected) {
            FileLogger.error("Cannot send request: Not connected");
            return;
        }
        try {
            String json = JsonConverter.toJson(request) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                int written = clientChannel.write(buffer);
                if (written == 0) {
                    Thread.onSpinWait();
                }
            }
        } catch (IOException e) {
            FileLogger.error("Failed to send request", e);
            disconnect();
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (selector != null) selector.close();
            if (clientChannel != null) clientChannel.close();
            FileLogger.info("Disconnected from NIO Server.");
        } catch (IOException e) {
            FileLogger.error("Error during disconnect", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
