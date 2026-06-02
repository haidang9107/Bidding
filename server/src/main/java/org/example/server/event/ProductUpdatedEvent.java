package org.example.server.event;

/**
 * Event triggered when a product's information is updated.
 */
public record ProductUpdatedEvent(int productId) implements DomainEvent {
}
