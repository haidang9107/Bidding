package org.example.client.watchlist;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory tracking of "products I am interested in" (watchlist).
 *
 * Pure client-side state — does not modify any existing server logic.
 * Persists for the lifetime of the JVM only.
 *
 * Used by AuctionList to render the "Sản phẩm mình quan tâm" tab.
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
            fire();
        }
    }

    public void remove(int productId) {
        if (watched.remove(productId)) {
            fire();
        }
    }

    public void toggle(int productId) {
        synchronized (watched) {
            if (!watched.add(productId)) {
                watched.remove(productId);
            }
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

    private void fire() {
        Set<Integer> snap = all();
        for (Consumer<Set<Integer>> l : listeners) {
            try { l.accept(snap); } catch (Exception ignored) {}
        }
    }
}
