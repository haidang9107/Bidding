package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.dto.request.BidRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to place a manual bid on an auction.
 * Events are published by the BidService, so manual broadcast is not required here.
 */
public class BidPlaceCommand implements Command {
    private final BidController bidController;

    /**
     * Constructs a BidPlaceCommand with the specified BidController.
     *
     * @param bidController the controller for bidding operations
     */
    public BidPlaceCommand(BidController bidController) {
        this.bidController = bidController;
    }

    /**
     * Executes the bid placement command.
     *
     * @param request the request containing BidRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the bid
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        BidRequest bidRequest = JsonConverter.convert(request.getPayload(), BidRequest.class);
        return bidController.handlePlaceBid(bidRequest, currentUser.getAccountname());
    }
}
