package org.example.server.controller;

import org.example.dto.request.AutoBidRequest;
import org.example.dto.request.BidHistoryRequest;
import org.example.dto.request.BidRequest;
import org.example.dto.response.BidResult;
import org.example.dto.response.PagedResponse;
import org.example.model.Bid;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.bid.BidService;

/**
 * Controller for handling bidding requests.
 */
public class BidController {
    private final BidService bidService;

    /**
     * Constructs a BidController.
     * @param bidService the bid service.
     */
    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    /**
     * Handles retrieving the bid history for an auction with pagination.
     * @param historyReq The bid history request details.
     * @return A response containing the paged list of bids.
     */
    public Response<PagedResponse<Bid>> handleGetBidHistory(BidHistoryRequest historyReq) {
        if (historyReq == null) {
            return new Response<>(MessageType.ERROR, false, "History request data required", null);
        }
        PagedResponse<Bid> history = bidService.getBidHistoryPaged(
                historyReq.getAuctionId(),
                historyReq.getPage(),
                historyReq.getPageSize()
        );
        return new Response<>(MessageType.BID_HISTORY, true, "Bid history fetched successfully", history);
    }

    /**
     * Handles bid placement.
     * @param bidReq the bid request details.
     * @param authenticatedAccountname the account name of the authenticated user.
     * @return a response indicating the result of the bid placement.
     */
    public Response<BidResult> handlePlaceBid(BidRequest bidReq, String authenticatedAccountname) {
        if (bidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Bid data required", null);
        }
        BidResult result = bidService.placeBid(bidReq.getAuctionId(), authenticatedAccountname, bidReq.getAmount());
        return new Response<>(MessageType.SUCCESS, true, "Bid placed successfully", result);
    }

    /**
     * Configures automatic bidding for an auction.
     * @param autoBidReq The auto-bid configuration request.
     * @param authenticatedAccountname The account name of the bidder.
     * @return A success response.
     */
    public Response<String> handleConfigureAutoBid(AutoBidRequest autoBidReq, String authenticatedAccountname) {
        if (autoBidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Auto bid data required", null);
        }
        bidService.configureAutoBid(
                autoBidReq.getAuctionId(),
                authenticatedAccountname,
                autoBidReq.getMaxBid(),
                autoBidReq.getIncrementAmount()
        );
        return new Response<>(MessageType.SUCCESS, true, "Auto bid configured successfully", null);
    }

    /**
     * Handles the cancellation of an automatic bid configuration.
     * @param autoBidReq the auto-bid request details.
     * @param authenticatedAccountname the account name of the authenticated user.
     * @return a response indicating the result of the cancellation.
     */
    public Response<String> handleCancelAutoBid(AutoBidRequest autoBidReq, String authenticatedAccountname) {
        if (autoBidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Auto bid data required", null);
        }
        bidService.cancelAutoBid(autoBidReq.getAuctionId(), authenticatedAccountname);
        return new Response<>(MessageType.SUCCESS, true, "Auto bid canceled successfully", null);
    }
}
