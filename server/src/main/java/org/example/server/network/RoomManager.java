package org.example.server.network;

import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks realtime subscribers for each auction session.
 */
public class RoomManager {
    private static final ConcurrentHashMap<Integer, Set<SocketChannel>> auctionRooms =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<SocketChannel, Set<Integer>> channelRooms =
            new ConcurrentHashMap<>();

    private RoomManager() {
    }

    /**
     * Adds a channel to an auction room.
     * @param auctionId the ID of the auction
     * @param channel the client's socket channel
     */
    public static void joinAuction(int auctionId, SocketChannel channel) {
        auctionRooms.computeIfAbsent(auctionId, ignored -> ConcurrentHashMap.newKeySet()).add(channel);
        channelRooms.computeIfAbsent(channel, ignored -> ConcurrentHashMap.newKeySet()).add(auctionId);
    }

    /**
     * Removes a channel from an auction room.
     * @param auctionId the ID of the auction
     * @param channel the client's socket channel
     */
    public static void leaveAuction(int auctionId, SocketChannel channel) {
        Set<SocketChannel> clients = auctionRooms.get(auctionId);
        if (clients != null) {
            clients.remove(channel);
            if (clients.isEmpty()) {
                auctionRooms.remove(auctionId);
            }
        }

        Set<Integer> rooms = channelRooms.get(channel);
        if (rooms != null) {
            rooms.remove(auctionId);
            if (rooms.isEmpty()) {
                channelRooms.remove(channel);
            }
        }
    }

    /**
     * Removes a channel from all auction rooms it belongs to.
     * @param channel the client's socket channel
     */
    public static void removeChannel(SocketChannel channel) {
        Set<Integer> rooms = channelRooms.remove(channel);
        if (rooms == null) {
            return;
        }
        for (Integer auctionId : rooms) {
            Set<SocketChannel> clients = auctionRooms.get(auctionId);
            if (clients != null) {
                clients.remove(channel);
                if (clients.isEmpty()) {
                    auctionRooms.remove(auctionId);
                }
            }
        }
    }

    /**
     * Gets all client channels subscribed to an auction room.
     * @param auctionId the ID of the auction
     * @return a set of socket channels
     */
    public static Set<SocketChannel> getAuctionClients(int auctionId) {
        return auctionRooms.getOrDefault(auctionId, Collections.emptySet());
    }

    /**
     * Gets the number of clients in an auction room.
     * @param auctionId the ID of the auction
     * @return the number of clients
     */
    public static int getAuctionClientCount(int auctionId) {
        return getAuctionClients(auctionId).size();
    }
}
