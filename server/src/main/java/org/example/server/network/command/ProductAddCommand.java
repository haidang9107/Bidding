package org.example.server.network.command;

import org.example.dto.ProductAddRequest;
import org.example.model.product.*;
import org.example.model.enums.ItemCategory;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.ProductController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;
import java.sql.Timestamp;

public class ProductAddCommand implements Command {
    private final ProductController productController;

    public ProductAddCommand(ProductController productController) {
        this.productController = productController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        try {
            ProductAddRequest addReq = JsonConverter.fromJson(JsonConverter.toJson(request.getPayload()), ProductAddRequest.class);
            if (addReq == null) {
                return new Response<>(MessageType.ERROR, false, "Invalid product data", null);
            }

            User currentUser = SessionManager.getUser(channel);
            int sellerId = (currentUser != null) ? currentUser.getUserId() : 0;

            Item item = switch (addReq.getCategory()) {
                case ELECTRONICS -> JsonConverter.fromJson(JsonConverter.toJson(addReq), Electronics.class);
                case ART -> JsonConverter.fromJson(JsonConverter.toJson(addReq), Art.class);
                case VEHICLE -> JsonConverter.fromJson(JsonConverter.toJson(addReq), Vehicle.class);
            };
            
            // Initialize mandatory auction fields
            item.setSellerId(sellerId);
            item.setStatus(AuctionStatus.OPEN);
            item.setCurrentPrice(item.getStartingPrice());
            item.setStepPrice(item.getStartingPrice() / 10); // Default step price
            
            if (item.getStartTime() == null) {
                item.setStartTime(new Timestamp(System.currentTimeMillis()));
            }
            if (item.getEndTime() == null) {
                // Default end time: 7 days from now
                item.setEndTime(new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L));
            }

            boolean success = productController.handleCreateAuction(item);
            return new Response<>(MessageType.SUCCESS, success, success ? "Product added" : "Failed to add", null);
        } catch (Exception e) {
            return new Response<>(MessageType.ERROR, false, "Error adding product: " + e.getMessage(), null);
        }
    }
}
