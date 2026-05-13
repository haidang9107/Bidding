package org.example.client.network;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class SocketClient {
    private static SocketClient instance;
    private SocketChannel socketChannel;
    private Selector selector;
    private volatile boolean connected = false;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final StringBuilder inputBuffer = new StringBuilder();

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (connected) return;

        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        new Thread(this::runSelector).start();

        System.out.println(">>> Connecting to server at " + host + ":" + port);
    }

    private void runSelector() {
        try {
            while (selector.isOpen()) {
                if (selector.select() == 0) continue;

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(">>> Selector error: " + e.getMessage());
            disconnect();
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        connected = true;
        System.out.println(">>> Connected to server.");
    }

    private void handleRead(SelectionKey key) throws IOException {
        int bytesRead = socketChannel.read(readBuffer);
        if (bytesRead == -1) {
            throw new IOException("Server closed connection");
        }

        readBuffer.flip();
        String received = StandardCharsets.UTF_8.decode(readBuffer).toString();
        inputBuffer.append(received);
        readBuffer.clear();

        processInput();
    }

    private void processInput() {
        int newlineIndex;
        while ((newlineIndex = inputBuffer.indexOf("\n")) != -1) {
            String message = inputBuffer.substring(0, newlineIndex).trim();
            inputBuffer.delete(0, newlineIndex + 1);

            if (!message.isEmpty()) {
                Response response = JsonConverter.fromJson(message, Response.class);
                handleResponse(response);
            }
        }
    }

    public void sendRequest(Request request) {
        if (!connected) {
            System.err.println(">>> Not connected to server!");
            return;
        }
        try {
            String json = JsonConverter.toJson(request) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        } catch (IOException e) {
            System.err.println(">>> Error sending request: " + e.getMessage());
            disconnect();
        }
    }

    private void handleResponse(Response response) {
        System.out.println(">>> Received from server: " + response.getMessage());
    }

    public void disconnect() {
        try {
            connected = false;
            if (selector != null) selector.close();
            if (socketChannel != null) socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
