package org.example.server.event;

import org.example.dto.notify.AuctionEndNotify;
import org.example.dto.notify.BidUpdateNotify;
import org.example.dto.notify.ProductUpdateNotify;
import org.example.dto.response.ProductResponse;
import org.example.model.Auction;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.network.Broadcaster;
import org.example.server.service.auction.AuctionService;

/**
 * Listens for domain events and pushes notifications to clients via the
 * {@link Broadcaster}.
 */
public class NetworkNotificationListener {

    private final AuctionService auctionService;

    /**
     * Constructs a listener.
     * @param auctionService The auction service used to fetch details on demand.
     */
    public NetworkNotificationListener(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Registers all event listeners with the specified publisher.
     * @param publisher The event publisher to subscribe to.
     */
    public void registerAll(EventPublisher publisher) {
        publisher.subscribe(NewBidPlacedEvent.class,    this::onNewBid);
        publisher.subscribe(AuctionStartedEvent.class,  this::onAuctionStarted);
        publisher.subscribe(AuctionEndedEvent.class,    this::onAuctionEnded);
        publisher.subscribe(AuctionCreatedEvent.class,  this::onAuctionCreated);
        publisher.subscribe(BalanceChangedEvent.class,  this::onBalanceChanged);
    }

    private void onBalanceChanged(BalanceChangedEvent e) {
        Broadcaster.sendToUser(e.accountname(), new Response<>(
                MessageType.BALANCE_UPDATE, true, "Số dư đã thay đổi",
                new org.example.dto.response.BalanceResponse(e.accountname(), e.newBalance(), e.newBlockedBalance())));
    }

    private void onNewBid(NewBidPlacedEvent e) {
        // Broadcast to the room
        Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                MessageType.BID_UPDATE, true, "New highest bid",
                new BidUpdateNotify(e.auctionId(), e.winnerAccountname(),
                        e.currentPrice(), e.autoBidApplied(), e.newEndTime())));

        // Private outbid notification
        if (e.oldWinnerAccount() != null && !e.oldWinnerAccount().equals(e.winnerAccountname())) {
            Auction auction = auctionService.getAuctionById(e.auctionId());
            String itemName = auction != null && auction.getProduct() != null 
                ? auction.getProduct().getName() : "sản phẩm";
            
            Broadcaster.sendToUser(e.oldWinnerAccount(), new Response<>(
                    MessageType.NOTIFICATION, true,
                    "Bạn đã bị vượt mặt tại đấu giá: " + itemName + ". Hãy quay lại trả giá ngay!", null));
        }
    }

    private void onAuctionStarted(AuctionStartedEvent e) {
        Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                MessageType.AUCTION_START, true,
                "Auction '" + e.itemName() + "' has started!", null));
    }

    private void onAuctionEnded(AuctionEndedEvent e) {
        if (e.canceled()) {
            Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                    MessageType.NOTIFICATION, true,
                    "Auction " + e.auctionId() + " has been canceled.", null));
        } else if (e.winnerAccountname() != null) {
            Auction auction = auctionService.getAuctionById(e.auctionId());
            Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                    MessageType.AUCTION_END, true,
                    "Auction for '" + e.itemName() + "' has ended!",
                    new AuctionEndNotify(e.auctionId(), e.winnerAccountname(),
                            e.finalPrice(), e.itemName(),
                            auction != null ? new ProductResponse(auction) : null)));
        } else {
            Broadcaster.broadcastToAuction(e.auctionId(), new Response<>(
                    MessageType.AUCTION_END, true,
                    "Auction for '" + e.itemName() + "' has ended with no winner.",
                    new AuctionEndNotify(e.auctionId(), null,
                            e.finalPrice(), e.itemName(), null)));
        }
    }

    private void onAuctionCreated(AuctionCreatedEvent e) {
        Auction auction = auctionService.getAuctionById(e.auctionId());
        if (auction == null) return;
        Broadcaster.broadcast(new Response<>(MessageType.PRODUCT_LIST, true,
                "New auction opened!", new ProductUpdateNotify(new ProductResponse(auction))));
    }
}
