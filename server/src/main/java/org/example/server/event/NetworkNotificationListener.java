package org.example.server.event;

import org.example.dto.notify.AuctionEndNotify;
import org.example.dto.notify.BidUpdateNotify;
import org.example.dto.notify.ProductUpdateNotify;
import org.example.dto.response.ProductResponse;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.network.Broadcaster;
import org.example.server.service.product.ProductService;

/**
 * Giai đoạn 1: Chịu trách nhiệm duy nhất là nhận Domain Events
 * và gửi thông báo tới clients qua Broadcaster.
 */
public class NetworkNotificationListener {

    private final ProductService productService;

    /**
     * Constructs a listener with the specified product service.
     * @param productService The product service to fetch details for notifications.
     */
    public NetworkNotificationListener(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Registers all event listeners with the specified publisher.
     * @param publisher The event publisher to subscribe to.
     */
    public void registerAll(EventPublisher publisher) {
        publisher.subscribe(NewBidPlacedEvent.class,    this::onNewBid);
        publisher.subscribe(AuctionStartedEvent.class,  this::onAuctionStarted);
        publisher.subscribe(AuctionEndedEvent.class,    this::onAuctionEnded);
        publisher.subscribe(ProductCreatedEvent.class,  this::onProductCreated);
    }

    /**
     * Handles new bid events.
     * @param e The event.
     */
    private void onNewBid(NewBidPlacedEvent e) {
        Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                MessageType.BID_UPDATE, true, "New highest bid",
                new BidUpdateNotify(e.auctionId(), e.winnerAccountname(),
                        e.currentPrice(), e.autoBidApplied(), e.newEndTime())));
    }

    /**
     * Handles auction started events.
     * @param e The event.
     */
    private void onAuctionStarted(AuctionStartedEvent e) {
        Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                MessageType.AUCTION_START, true,
                "Auction '" + e.itemName() + "' has started!", null));
    }

    /**
     * Handles auction ended events.
     * @param e The event.
     */
    private void onAuctionEnded(AuctionEndedEvent e) {
        if (e.canceled()) {
            Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                    MessageType.NOTIFICATION, true,
                    "Auction " + e.auctionId() + " has been canceled.", null));
        } else if (e.winnerAccountname() != null) {
            var item = productService.getAuctionById(e.auctionId());
            Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                    MessageType.AUCTION_END, true,
                    "Auction for '" + e.itemName() + "' has ended!",
                    new AuctionEndNotify(e.auctionId(), e.winnerAccountname(),
                            e.finalPrice(), e.itemName(),
                            item != null ? new ProductResponse(item) : null)));
        }
    }

    /**
     * Handles product created events.
     * @param e The event.
     */
    private void onProductCreated(ProductCreatedEvent e) {
        var item = productService.getAuctionById(e.auctionId());
        if (item == null) return;
        Broadcaster.broadcast(new Response<>(MessageType.PRODUCT_LIST, true,
                "New product added!", new ProductUpdateNotify(new ProductResponse(item))));
    }
}
