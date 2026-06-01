package org.example.client.watchlist;

import com.google.gson.JsonObject;
import org.example.client.network.SocketClient;
import org.example.model.enums.MessageType;
import org.example.payload.Request;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tracking of "products I am interested in" (watchlist), now backed by the
 * server (WATCHLIST_ADD / WATCHLIST_REMOVE / WATCHLIST_GET).
 *
 * <p>The local Set is a cache for instant UI feedback (heart toggle); the
 * authoritative state lives on the server. {@link #refreshFromServer()}
 * requests the server copy, and {@link #syncFromServer(Set)} applies the
 * WATCHLIST_GET response. add/remove update the cache optimistically AND
 * tell the server.</p>
 *
 * Singleton.
 */
public final class WatchlistManager {

    private static final WatchlistManager INSTANCE = new WatchlistManager();

    public static WatchlistManager getInstance() {
        return INSTANCE;
    }

    private final Set<Integer> watched = Collections.synchronizedSet(new LinkedHashSet<>());
    private final CopyOnWriteArrayList<Consumer<Set<Integer>>> listeners = new CopyOnWriteArrayList<>();

    private WatchlistManager() {
    }

    public boolean isWatched(int productId) {
        return watched.contains(productId);
    }

    public void add(int productId) {
        if (watched.add(productId)) {
            sendToServer(MessageType.WATCHLIST_ADD, productId);
            fire();
        }
    }

    public void remove(int productId) {
        if (watched.remove(productId)) {
            sendToServer(MessageType.WATCHLIST_REMOVE, productId);
            fire();
        }
    }

    public void toggle(int productId) {
        boolean nowWatched;
        synchronized (watched) {
            if (watched.add(productId)) {
                nowWatched = true;
            } else {
                watched.remove(productId);
                nowWatched = false;
            }
        }
        sendToServer(nowWatched ? MessageType.WATCHLIST_ADD : MessageType.WATCHLIST_REMOVE, productId);
        fire();
    }

    /** Ask the server for the authoritative watchlist (response handled by
     *  whichever controller is listening, which then calls syncFromServer). */
    public void refreshFromServer() {
        try {
            SocketClient.getInstance().send(new Request(MessageType.WATCHLIST_GET, null));
        } catch (Exception ignored) {}
    }

    /** Replace the local cache with the server's set of product ids. */
    public void syncFromServer(Set<Integer> serverIds) {
        synchronized (watched) {
            watched.clear();
            if (serverIds != null) watched.addAll(serverIds);
        }
        fire();
    }

    /** Read-only snapshot. */
    public Set<Integer> all() {
        synchronized (watched) {
            return new LinkedHashSet<>(watched);
        }
    }

    public void subscribe(Consumer<Set<Integer>> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<Set<Integer>> listener) {
        listeners.remove(listener);
    }

    private void sendToServer(MessageType type, int productId) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("productId", productId);
            SocketClient.getInstance().send(new Request(type, payload));
        } catch (Exception ignored) {
            // Network hiccup — local cache still updated; a later
            // refreshFromServer() will reconcile.
        }
    }

    private void fire() {
        Set<Integer> snap = all();
        for (Consumer<Set<Integer>> l : listeners) {
            try { l.accept(snap); } catch (Exception ignored) {}
        }
    }
}
