package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve detailed information about a specific auction.
 */
public class ProductDetailCommand implements Command {
    private final ProductController productController;

    /**
     * Constructs a ProductDetailCommand with the specified ProductController.
     *
     * @param productController the controller for product-related operations
     */
    public ProductDetailCommand(ProductController productController) {
        this.productController = productController;
    }

    /**
     * Executes the product detail command.
     *
     * @param request the request containing the auction ID
     * @param channel the socket channel of the client
     * @return the response containing auction details
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest detailRequest = JsonConverter.convert(request.getPayload(), AuctionRoomRequest.class);
        if (detailRequest == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid request payload", null);
        }
        return productController.handleGetAuctionDetail(detailRequest.getAuctionId());
    }
}
