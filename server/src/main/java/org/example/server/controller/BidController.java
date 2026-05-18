package org.example.server.controller;

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
     * Payload format: "productId:bidderId:amount"
     */
    public Response<String> handlePlaceBid(Object payload) {
        if (payload == null) {
            return new Response<>(MessageType.ERROR, false, "Bid data required", null);
        }

        String[] data = payload.toString().split(":");
        if (data.length < 3) {
            return new Response<>(MessageType.ERROR, false, "Invalid bid format. Use 'productId:bidderId:amount'", null);
        }

        try {
            int productId = Integer.parseInt(data[0]);
            int bidderId = Integer.parseInt(data[1]);
            long amount = Long.parseLong(data[2]);

            String result = bidService.placeBid(productId, bidderId, amount);
            
            if ("SUCCESS".equals(result)) {
                return new Response<>(MessageType.SUCCESS, true, "Bid placed successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, result, null);
            }
        } catch (NumberFormatException e) {
            return new Response<>(MessageType.ERROR, false, "Invalid data format in bid request", null);
        }
    }
}
