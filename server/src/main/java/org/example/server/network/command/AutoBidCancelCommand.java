package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.AutoBidRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to cancel an existing auto-bid configuration for an auction.
 */
@RequiresRole(UserRole.MEMBER)
public class AutoBidCancelCommand implements Command {
    private final BidController bidController;

    /**
     * Constructs an AutoBidCancelCommand with the specified BidController.
     *
     * @param bidController the controller for bidding operations
     */
    public AutoBidCancelCommand(BidController bidController) {
        this.bidController = bidController;
    }

    /**
     * Executes the cancel auto-bid command.
     *
     * @param request the request containing AutoBidRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the cancellation
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        
        AutoBidRequest autoBidReq = JsonConverter.convert(request.getPayload(), AutoBidRequest.class);
        return bidController.handleCancelAutoBid(autoBidReq, currentUser.getAccountname());
    }
}
