package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;

import java.nio.channels.SocketChannel;

public class ProductListCommand implements Command {
    private final ProductController productController;

    public ProductListCommand(ProductController productController) {
        this.productController = productController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return productController.handleGetAllAuctions(request.getPayload());
    }
}
