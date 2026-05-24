package org.example.server.event;

import org.example.util.FileLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Giai đoạn 1: Event Bus đồng bộ, thread-safe.
 */
public class EventPublisher {
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Subscribes a listener to a specific event type.
     * @param <E> The event type.
     * @param eventType The class of the event.
     * @param listener The listener to invoke when the event is published.
     */
    public <E extends DomainEvent> void subscribe(Class<E> eventType, EventListener<E> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Publishes an event to all subscribed listeners.
     * @param <E> The event type.
     * @param event The event object.
     */
    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        List<EventListener<?>> handlers = listeners.getOrDefault(event.getClass(), List.of());
        for (EventListener<?> handler : handlers) {
            try {
                ((EventListener<E>) handler).onEvent(event);
            } catch (Exception e) {
                FileLogger.error("EventListener error for " + event.getClass().getSimpleName(), e);
            }
        }
    }
}
