package org.example.client.watchlist;

import org.example.client.notification.NotificationService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory tracking of "products I am currently bidding on".
 *
 * Whenever the user places a bid (or the controller observes one), we register
 * the product here. On every BID_UPDATE we then know whether the new leader is
 * still us (status = WINNING) or someone else (status = OUTBID) and can raise
 * a notification automatically.
 *
 * Singleton, in-memory only. Does not change any server / DB logic.
 */
public final class MyBidsManager {

    private static final MyBidsManager INSTANCE = new MyBidsManager();

    public static MyBidsManager getInstance() {
        return INSTANCE;
    }

    public enum Status { WINNING, OUTBID, WON, LOST, PENDING }

    /** A single tracked bid entry. */
    public static final class Entry {
        public final int productId;
        public String productName;
        public long  myLastBid;
        public long  currentPrice;
        public String currentLeader;
        public Status status;
        public long  lastUpdateMillis;

        Entry(int productId) {
            this.productId = productId;
            this.status = Status.PENDING;
            this.lastUpdateMillis = System.currentTimeMillis();
        }
    }

    private final Map<Integer, Entry> entries =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private final CopyOnWriteArrayList<Consumer<Map<Integer, Entry>>> listeners =
            new CopyOnWriteArrayList<>();

    private MyBidsManager() {
    }

    // ============================================================
    // Mutators
    // ============================================================

    /** Register that the current user placed a bid on the given product. */
    public synchronized void recordMyBid(int productId, String productName,
                                         long myBid) {
        Entry e = entries.computeIfAbsent(productId, Entry::new);
        e.productName = (productName != null && !productName.isEmpty())
                ? productName : e.productName;
        e.myLastBid = myBid;
        e.currentPrice = Math.max(e.currentPrice, myBid);
        e.status = Status.WINNING;
        e.lastUpdateMillis = System.currentTimeMillis();
        fire();
    }

    /**
     * Called whenever the price for a tracked product changes (BID_UPDATE).
     *
     * @param productId        product id
     * @param productName      display name (optional)
     * @param newPrice         current highest price
     * @param newLeaderName    accountname of current leader, may be null
     * @param myAccountname    accountname of the local user (to compare)
     * @return true if we changed status to OUTBID and fired a notification
     */
    public synchronized boolean onPriceUpdate(int productId, String productName,
                                              long newPrice, String newLeaderName,
                                              String myAccountname) {
        Entry e = entries.get(productId);
        if (e == null) return false; // not bidding on this product

        if (productName != null && !productName.isEmpty()) e.productName = productName;
        e.currentPrice = newPrice;
        e.currentLeader = newLeaderName;
        e.lastUpdateMillis = System.currentTimeMillis();

        boolean leaderIsMe = newLeaderName != null
                && myAccountname != null
                && newLeaderName.equalsIgnoreCase(myAccountname);

        Status before = e.status;
        e.status = leaderIsMe ? Status.WINNING : Status.OUTBID;
        fire();

        if (!leaderIsMe && before != Status.OUTBID) {
            NotificationService.getInstance().outbid(
                    "Bạn đã bị vượt giá!",
                    "Sản phẩm \"" + safe(e.productName, "#" + productId)
                            + "\" đang ở giá " + format(newPrice)
                            + (newLeaderName == null ? "" : " (dẫn đầu: " + newLeaderName + ")"));
            return true;
        }
        return false;
    }

    /** Called when an auction ends so the entry can be marked WON / LOST. */
    public synchronized void onAuctionEnd(int productId, String winnerName,
                                          long finalPrice, String myAccountname) {
        Entry e = entries.get(productId);
        if (e == null) return;
        e.currentPrice = finalPrice;
        e.currentLeader = winnerName;
        e.lastUpdateMillis = System.currentTimeMillis();
        boolean won = winnerName != null && myAccountname != null
                && winnerName.equalsIgnoreCase(myAccountname);
        e.status = won ? Status.WON : Status.LOST;
        fire();
        if (won) {
            NotificationService.getInstance().win(
                    "🎉 Bạn đã thắng phiên đấu giá!",
                    "Sản phẩm \"" + safe(e.productName, "#" + productId)
                            + "\" với giá " + format(finalPrice));
        }
    }

    public synchronized void remove(int productId) {
        if (entries.remove(productId) != null) fire();
    }

    // ============================================================
    // Accessors
    // ============================================================
    public synchronized Map<Integer, Entry> snapshot() {
        return new LinkedHashMap<>(entries);
    }

    public boolean isTracking(int productId) {
        return entries.containsKey(productId);
    }

    public void subscribe(Consumer<Map<Integer, Entry>> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<Map<Integer, Entry>> listener) {
        listeners.remove(listener);
    }

    private void fire() {
        Map<Integer, Entry> snap = snapshot();
        for (Consumer<Map<Integer, Entry>> l : listeners) {
            try { l.accept(snap); } catch (Exception ignored) {}
        }
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static String format(long p) {
        return String.format("%,d đ", p);
    }
}
