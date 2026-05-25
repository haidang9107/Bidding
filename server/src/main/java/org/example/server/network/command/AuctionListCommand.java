package org.example.server.network.command;

import org.example.dto.request.PaginationRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuctionController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve a paginated list of all active or scheduled auctions.
 */
public class AuctionListCommand implements Command {
    private final AuctionController auctionController;

    /**
     * Constructs an AuctionListCommand.
     * @param auctionController The auction controller.
     */
    public AuctionListCommand(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    /**
     * Executes the auction list command.
     * @param request The request containing pagination info.
     * @param channel The socket channel of the client.
     * @return The response containing the list of auctions.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        PaginationRequest pagReq = JsonConverter.convert(request.getPayload(), PaginationRequest.class);
        return auctionController.handleGetAllAuctions(pagReq);
    }
}
