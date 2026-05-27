package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.ProductAddRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuctionController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to create a new product and open its first auction.
 */
@RequiresRole(UserRole.MEMBER)
public class AuctionCreateCommand implements Command {
    private final AuctionController auctionController;

    /**
     * Constructs an AuctionCreateCommand.
     * @param auctionController The auction controller.
     */
    public AuctionCreateCommand(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    /**
     * Executes the auction create command.
     * @param request The request containing the product/auction details.
     * @param channel The socket channel of the user.
     * @return The response indicating success or failure.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        ProductAddRequest addReq = JsonConverter.convert(request.getPayload(), ProductAddRequest.class);
        return auctionController.handleCreateAuction(addReq, currentUser.getAccountname());
    }
}
