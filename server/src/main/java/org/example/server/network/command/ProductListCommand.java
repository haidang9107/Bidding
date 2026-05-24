package org.example.server.network.command;

import org.example.dto.request.PaginationRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve a paginated list of all active or scheduled auctions.
 */
public class ProductListCommand implements Command {
    private final ProductController productController;

    /**
     * Constructs a ProductListCommand with the specified ProductController.
     *
     * @param productController the controller for product-related operations
     */
    public ProductListCommand(ProductController productController) {
        this.productController = productController;
    }

    /**
     * Executes the product list command.
     *
     * @param request the request containing PaginationRequest
     * @param channel the socket channel of the client
     * @return the response containing the list of auctions
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        PaginationRequest pagReq = JsonConverter.convert(request.getPayload(), PaginationRequest.class);
        return productController.handleGetAllAuctions(pagReq);
    }
}
