package org.example.server.event;

/**
 * Event published when a new auction is opened (after the underlying product is created
 * or selected for re-auction).
 *
 * @param auctionId The ID of the newly created auction.
 */
public record AuctionCreatedEvent(int auctionId) implements DomainEvent {}
