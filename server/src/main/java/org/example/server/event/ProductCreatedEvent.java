package org.example.server.event;

/**
 * Event published when a new product is created and placed in auction.
 * @param auctionId The ID of the new auction.
 */
public record ProductCreatedEvent(int auctionId) implements DomainEvent {}
