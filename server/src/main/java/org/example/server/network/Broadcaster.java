package org.example.server.network;

import org.example.payload.Response;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages broadcasting messages to all connected clients on the single port.
 */
public class Broadcaster {
    private static final List<SocketChannel> clients = new CopyOnWriteArrayList<>();

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

    public static void broadcastToAuction(int auctionId, Response<?> response) {
        writeToChannels(RoomManager.getAuctionClients(auctionId), response);
    }

    private static void writeToChannels(Collection<SocketChannel> channels, Response<?> response) {
        String jsonMessage = JsonConverter.toJson(response) + "\n";
        byte[] messageBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);

        for (SocketChannel channel : channels) {
            if (!channel.isOpen()) {
                clients.remove(channel);
                RoomManager.removeChannel(channel);
                continue;
            }

            try {
                // Synchronize on the channel to prevent interleaved writes from different threads
                synchronized (channel) {
                    ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }
            } catch (IOException e) {
                FileLogger.error("Failed to send broadcast to channel: " + channel, e);
                clients.remove(channel);
                RoomManager.removeChannel(channel);
                try {
                    channel.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
