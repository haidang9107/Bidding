package org.example.server.event;

/**
 * Event published when an auction is cancelled.
 * @param auctionId The ID of the cancelled auction.
 */
public record AuctionCancelledEvent(int auctionId) implements DomainEvent {}
