package org.example.server.controller;

import org.example.dto.BidRequest;
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
    public Response<String> handlePlaceBid(Object payload) {
        BidRequest bidReq = JsonConverter.fromJson(JsonConverter.toJson(payload), BidRequest.class);
        if (bidReq == null) {
            return new Response<>(MessageType.ERROR, false, "Bid data required", null);
        }

        String result = bidService.placeBid(bidReq.getProductId(), bidReq.getBidderAccountname(), bidReq.getAmount());
        
        if ("SUCCESS".equals(result)) {
            return new Response<>(MessageType.SUCCESS, true, "Bid placed successfully", null);
        } else {
            return new Response<>(MessageType.ERROR, false, result, null);
        }
    }
}
