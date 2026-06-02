package org.example.server.network.command;

import org.example.dto.request.ProductSearchRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuctionController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to search and filter products/auctions with advanced criteria.
 * Handles the {@link MessageType#PRODUCT_SEARCH} request.
 */
public class ProductSearchCommand implements Command {
    private final AuctionController auctionController;

    /**
     * Constructs a ProductSearchCommand.
     * @param auctionController The controller to delegate search logic to.
     */
    public ProductSearchCommand(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    /**
     * Executes the search request.
     * @param request The search request containing {@link ProductSearchRequest}.
     * @param channel The sender's socket channel.
     * @return A response containing a {@link PagedResponse} of search results.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        ProductSearchRequest searchReq = JsonConverter.convert(request.getPayload(), ProductSearchRequest.class);
        return auctionController.handleSearchAuctions(searchReq);
    }
}
