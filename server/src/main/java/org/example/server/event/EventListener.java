package org.example.server.event;

/** Listener nhận một loại event cụ thể. */
@FunctionalInterface
public interface EventListener<E extends DomainEvent> {
    /**
     * Handles the event.
     * @param event The event to handle.
     */
    void onEvent(E event);
}
