package org.example.server.controller;

import org.example.dto.request.AutoBidRequest;
import org.example.dto.request.BidRequest;
import org.example.dto.response.BidResult;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.bid.BidService;

/**
 * Controller for handling bidding requests.
 */
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    /**
     * Handles bid placement.
     */
    public Response<BidResult> handlePlaceBid(BidRequest bidReq, String authenticatedAccountname) {
        if (bidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Bid data required", null);
        }
        
        BidResult result = bidService.placeBid(bidReq.getAuctionId(), authenticatedAccountname, bidReq.getAmount());
        return new Response<>(MessageType.SUCCESS, true, "Bid placed successfully", result);
    }

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

    public Response<String> handleCancelAutoBid(AutoBidRequest autoBidReq, String authenticatedAccountname) {
        if (autoBidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Auto bid data required", null);
        }

        bidService.cancelAutoBid(autoBidReq.getAuctionId(), authenticatedAccountname);
        return new Response<>(MessageType.SUCCESS, true, "Auto bid canceled successfully", null);
    }
}
