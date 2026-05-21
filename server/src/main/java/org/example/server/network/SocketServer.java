package org.example.server.network;

import org.example.server.network.command.CommandRegistry;
import org.example.server.repository.DatabaseManager;
import org.example.util.Config;
import org.example.util.FileLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance Non-blocking Socket Server using Java NIO.
 * Unified single-port version.
 */
public class SocketServer {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final CommandRegistry commandRegistry;

    private final Map<SocketChannel, ByteArrayOutputStream> clientBuffers = new ConcurrentHashMap<>();
    private final InactivityMonitor inactivityMonitor = new InactivityMonitor(60); // 60 seconds timeout
    private final AuctionMonitor auctionMonitor;

    public SocketServer(CommandRegistry commandRegistry, AuctionMonitor auctionMonitor) {
        this.port = Config.getInt("SERVER_PORT");
        this.commandRegistry = commandRegistry;
        this.auctionMonitor = auctionMonitor;
    }

    public void run(String... args) {
        try {
            inactivityMonitor.start();
            auctionMonitor.start();
            selector = Selector.open();

            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            FileLogger.info("NIO Server started on Port: " + port);

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
                        executorService.submit(new CommandHandler(clientChannel, message, commandRegistry));
                    }
                } else {
                    buffer.write(b);
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
        } catch (IOException e) {
            FileLogger.error("Error stopping server", e);
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
            DatabaseManager.closeConnection();
        }
    }
}
ction();
        }
    }
}
