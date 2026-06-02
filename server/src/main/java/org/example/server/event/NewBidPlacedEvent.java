package org.example.server.event;

import java.sql.Timestamp;

/** Phát ra sau khi một bid (thủ công hoặc auto) được chấp nhận thành công. */
public record NewBidPlacedEvent(
        int     auctionId,
        String  winnerAccountname,
        String  oldWinnerAccount,
        long    currentPrice,
        boolean autoBidApplied,
        Timestamp newEndTime
) implements DomainEvent {}
