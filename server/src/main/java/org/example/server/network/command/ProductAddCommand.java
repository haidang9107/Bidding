package org.example.server.network.command;

import org.example.dto.request.ProductAddRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class ProductAddCommand implements Command {
    private final ProductController productController;

    public ProductAddCommand(ProductController productController) {
        this.productController = productController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        
        ProductAddRequest addReq = JsonConverter.convert(request.getPayload(), ProductAddRequest.class);
        return productController.handleCreateAuction(addReq, currentUser.getAccountname());
    }
}
