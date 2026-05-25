package org.example.server.event;

/** Phát ra khi một phiên đấu giá chuyển sang trạng thái RUNNING. */
public record AuctionStartedEvent(int auctionId, String itemName) implements DomainEvent {}
