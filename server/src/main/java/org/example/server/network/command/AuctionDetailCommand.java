package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuctionController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve detailed information about a specific auction.
 */
public class AuctionDetailCommand implements Command {
    private final AuctionController auctionController;

    /**
     * Constructs an AuctionDetailCommand.
     * @param auctionController The auction controller.
     */
    public AuctionDetailCommand(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    /**
     * Executes the auction detail command.
     * @param request The request containing the auction ID.
     * @param channel The socket channel of the client.
     * @return The response containing the auction details.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest detailRequest = JsonConverter.convert(request.getPayload(), AuctionRoomRequest.class);
        if (detailRequest == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid request payload", null);
        }
        return auctionController.handleGetAuctionDetail(detailRequest.getAuctionId());
    }
}
