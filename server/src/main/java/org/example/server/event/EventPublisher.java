package org.example.server.event;

import org.example.util.FileLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Giai đoạn 2: Event Bus bất đồng bộ (Asynchronous).
 * Giúp giải phóng thread chính và database transaction sớm hơn.
 */
public class EventPublisher {
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

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
     * Publishes an event to all subscribed listeners asynchronously.
     * Each listener's {@code onEvent} method is executed in a separate task
     * within the thread pool, ensuring the caller (e.g., a Service) 
     * is not blocked by slow notification processing.
     *
     * @param <E> The event type.
     * @param event The event object to publish.
     */
    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        List<EventListener<?>> handlers = listeners.getOrDefault(event.getClass(), List.of());
        for (EventListener<?> handler : handlers) {
            executorService.submit(() -> {
                try {
                    ((EventListener<E>) handler).onEvent(event);
                } catch (Exception e) {
                    FileLogger.error("EventListener error for " + event.getClass().getSimpleName(), e);
                }
            });
        }
    }

    /**
     * Shuts down the event publisher and its internal executor service.
     * Should be called during server shutdown to release threads.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
