package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class ProductDetailCommand implements Command {
    private final ProductController productController;

    public ProductDetailCommand(ProductController productController) {
        this.productController = productController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest detailRequest = JsonConverter.convert(request.getPayload(), AuctionRoomRequest.class);
        if (detailRequest == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid request payload", null);
        }
        return productController.handleGetAuctionDetail(detailRequest.getAuctionId());
    }
}
