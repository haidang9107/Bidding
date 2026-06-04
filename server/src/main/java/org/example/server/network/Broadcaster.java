package org.example.server.network;

import org.example.payload.Response;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages broadcasting messages to all connected clients on the single port.
 */
public class Broadcaster {
    private static final List<SocketChannel> clients = new CopyOnWriteArrayList<>();
    private static DisconnectionHandler disconnectionHandler;

    /**
     * Sets the disconnection handler to be used when a dead channel is detected.
     */
    public static void setDisconnectionHandler(DisconnectionHandler handler) {
        disconnectionHandler = handler;
    }

    /**
     * Adds a client channel to the broadcast list.
     */
    public static void addClient(SocketChannel channel) {
        if (!clients.contains(channel)) {
            clients.add(channel);
        }
    }

    /**
     * Removes a client channel from the broadcast list.
     */
    public static void removeClient(SocketChannel channel) {
        clients.remove(channel);
    }

    /**
     * Returns the total number of connected clients.
     */
    public static int getClientsCount() {
        return clients.size();
    }

    /**
     * Broadcasts a response message to all connected clients.
     */
    public static void broadcast(Response<?> response) {
        writeToChannels(clients, response);
    }

    /**
     * Broadcasts a response to all clients joined in a specific auction room.
     * @param auctionId The ID of the auction room.
     * @param response The response to broadcast.
     */
    public static void broadcastToAuction(int auctionId, Response<?> response) {
        writeToChannels(RoomManager.getAuctionClients(auctionId), response);
    }

    /**
     * Sends a response to a specific user by account name.
     * @param accountname The user's account name.
     * @param response The response to send.
     */
    public static void sendToUser(String accountname, Response<?> response) {
        SocketChannel channel = SessionManager.findChannelByUsername(accountname);
        if (channel != null && channel.isOpen()) {
            writeToChannels(List.of(channel), response);
        }
    }

    private static void writeToChannels(Collection<SocketChannel> channels, Response<?> response) {
        if (channels == null || channels.isEmpty()) {
            return;
        }

        String jsonMessage = JsonConverter.toJson(response) + "\n";
        byte[] messageBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
        List<SocketChannel> brokenChannels = new ArrayList<>();

        for (SocketChannel channel : channels) {
            if (!channel.isOpen()) {
                brokenChannels.add(channel);
                continue;
            }

            try {
                // Synchronize on the channel to prevent interleaved writes from different threads
                synchronized (channel) {
                    buffer.rewind();
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }
            } catch (IOException e) {
                FileLogger.error("Failed to send broadcast to channel: " + channel, e);
                brokenChannels.add(channel);
            }
        }

        if (!brokenChannels.isEmpty()) {
            for (SocketChannel badChannel : brokenChannels) {
                handleDisconnection(badChannel);
            }
        }
    }

    private static void handleDisconnection(SocketChannel channel) {
        if (disconnectionHandler != null) {
            disconnectionHandler.handle(channel);
        } else {
            // Fallback basic cleanup if handler is not injected
            clients.remove(channel);
            RoomManager.removeChannel(channel);
            try {
                channel.close();
            } catch (IOException ignored) {}
        }
    }
}