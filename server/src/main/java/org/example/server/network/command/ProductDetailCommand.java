package org.example.server.network.command;

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
        Integer productId = JsonConverter.convert(request.getPayload(), Integer.class);
        return productController.handleGetAuctionDetail(productId);
    }
}
