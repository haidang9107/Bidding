package org.example.server.event;

/** Phát ra khi một phiên đấu giá kết thúc (FINISHED hoặc CANCELED). */
public record AuctionEndedEvent(
        int    auctionId,
        String itemName,
        String winnerAccountname,  // null nếu không có người thắng
        long   finalPrice,
        boolean canceled
) implements DomainEvent {}
