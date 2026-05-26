package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.AutoBidRequest;
import org.example.dto.notify.AutoBidNotify;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.Broadcaster;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to set up or update auto-bidding for a specific auction.
 */
@RequiresRole(UserRole.MEMBER)
public class AutoBidSetCommand implements Command {
    private final BidController bidController;

    /**
     * Constructs an AutoBidSetCommand with the specified BidController.
     *
     * @param bidController the controller for bidding operations
     */
    public AutoBidSetCommand(BidController bidController) {
        this.bidController = bidController;
    }

    /**
     * Executes the set auto-bid command and broadcasts the change if successful.
     *
     * @param request the request containing AutoBidRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the configuration
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        AutoBidRequest autoBidRequest = JsonConverter.convert(request.getPayload(), AutoBidRequest.class);
        Response<?> response = bidController.handleConfigureAutoBid(
                autoBidRequest, currentUser.getAccountname());
                
        if (response.isSuccess() && autoBidRequest != null && autoBidRequest.getAuctionId() > 0) {
            Broadcaster.broadcastToAuction(
                    autoBidRequest.getAuctionId(),
                    new Response<>(
                            MessageType.NOTIFICATION,
                            true,
                            "Auto bid configured",
                            new AutoBidNotify(
                                    autoBidRequest.getAuctionId(),
                                    currentUser.getAccountname()
                            )
                    )
            );
        }
        return response;
    }
}
