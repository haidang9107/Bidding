package org.example.server.network;

import org.example.server.network.command.CommandRegistry;
import org.example.server.service.user.auth.AuthService;
import org.example.util.Config;
import org.example.util.FileLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SOLID: Single Responsibility - Manages the NIO Socket Server and client connections.
 */
public class SocketServer {
    private static final int PORT = Config.getInt("SERVER_PORT");
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64KB

    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final CommandRegistry commandRegistry;
    private final AuthService authService;
    private final AuctionMonitor auctionMonitor;
    private final ExecutorService executorService;
    private final Map<SocketChannel, ByteArrayOutputStream> clientBuffers = new HashMap<>();
    private boolean running = true;

    public SocketServer(CommandRegistry commandRegistry, AuthService authService, AuctionMonitor auctionMonitor) throws IOException {
        this.commandRegistry = commandRegistry;
        this.authService = authService;
        this.auctionMonitor = auctionMonitor;
        this.executorService = Executors.newFixedThreadPool(10); // Standard thread pool for commands

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(PORT));
        this.serverChannel.configureBlocking(false);
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        try {
            FileLogger.info("NIO Server started on Port: " + PORT);

            while (running) {
                if (selector.select() == 0) continue;

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept();
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            FileLogger.error("NIO Server Critical Failure: " + e.getMessage(), e);
        } finally {
            stop();
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        
        clientBuffers.put(clientChannel, new ByteArrayOutputStream());
        
        // Every connected client is added to Broadcaster to receive updates
        Broadcaster.addClient(clientChannel);
        
        FileLogger.info("New connection from: " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteArrayOutputStream buffer = clientBuffers.get(clientChannel);
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        try {
            int bytesRead = clientChannel.read(readBuffer);
            if (bytesRead == -1) {
                DisconnectionHandler.handle(clientChannel);
                key.cancel();
                clientBuffers.remove(clientChannel);
                return;
            }

            readBuffer.flip();
            while (readBuffer.hasRemaining()) {
                byte b = readBuffer.get();
                if (b == '\n') {
                    // Line complete: decode as UTF-8
                    String message = buffer.toString(StandardCharsets.UTF_8).trim();
                    buffer.reset();
                    if (!message.isEmpty()) {
                        executorService.submit(new CommandHandler(clientChannel, message, commandRegistry, authService));
                    }
                } else {
                    buffer.write(b);
                    if (buffer.size() > MAX_MESSAGE_SIZE) {
                        FileLogger.warn("Message size exceeded limit (64KB) from " + clientChannel.getRemoteAddress() + ". Disconnecting.");
                        DisconnectionHandler.handle(clientChannel);
                        key.cancel();
                        clientBuffers.remove(clientChannel);
                        return;
                    }
                }
            }
        } catch (IOException e) {
            FileLogger.error("Error reading from client: " + clientChannel, e);
            DisconnectionHandler.handle(clientChannel);
            key.cancel();
            clientBuffers.remove(clientChannel);
        }
    }

    public void stop() {
        running = false;
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
            executorService.shutdown();
        } catch (IOException e) {
            FileLogger.error("Error closing server: " + e.getMessage(), e);
        }
    }
}
