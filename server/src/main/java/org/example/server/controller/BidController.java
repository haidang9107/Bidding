package org.example.server.controller;

import org.example.dto.AutoBidRequest;
import org.example.dto.BidRequest;
import org.example.dto.BidResult;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.bid.BidService;
import org.example.util.JsonConverter;

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
    public Response<BidResult> handlePlaceBid(Object payload) {
        return handlePlaceBid(payload, null);
    }

    public Response<BidResult> handlePlaceBid(Object payload, String authenticatedAccountname) {
        BidRequest bidReq = JsonConverter.fromJson(JsonConverter.toJson(payload), BidRequest.class);
        if (bidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Bid data required", null);
        }
        if (authenticatedAccountname == null || authenticatedAccountname.isBlank()) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        BidResult result = bidService.placeBid(bidReq.getAuctionId(), authenticatedAccountname, bidReq.getAmount());
        
        if (result != null && result.getCurrentPrice() > 0) {
            return new Response<>(MessageType.SUCCESS, true, "Bid placed successfully", result);
        } else {
            return new Response<>(MessageType.ERROR, false, "Bid rejected", null);
        }
    }

    public Response<String> handleConfigureAutoBid(Object payload, String authenticatedAccountname) {
        AutoBidRequest autoBidReq = JsonConverter.fromJson(JsonConverter.toJson(payload), AutoBidRequest.class);
        if (autoBidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Auto bid data required", null);
        }
        if (authenticatedAccountname == null || authenticatedAccountname.isBlank()) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        String result = bidService.configureAutoBid(
                autoBidReq.getAuctionId(),
                authenticatedAccountname,
                autoBidReq.getMaxBid(),
                autoBidReq.getIncrementAmount()
        );
        if ("SUCCESS".equals(result)) {
            return new Response<>(MessageType.SUCCESS, true, "Auto bid configured successfully", null);
        }
        return new Response<>(MessageType.ERROR, false, result, null);
    }

    public Response<String> handleCancelAutoBid(Object payload, String authenticatedAccountname) {
        AutoBidRequest autoBidReq = JsonConverter.fromJson(JsonConverter.toJson(payload), AutoBidRequest.class);
        if (autoBidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Auto bid data required", null);
        }
        if (authenticatedAccountname == null || authenticatedAccountname.isBlank()) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        String result = bidService.cancelAutoBid(autoBidReq.getAuctionId(), authenticatedAccountname);
        if ("SUCCESS".equals(result)) {
            return new Response<>(MessageType.SUCCESS, true, "Auto bid canceled successfully", null);
        }
        return new Response<>(MessageType.ERROR, false, result, null);
    }
}
