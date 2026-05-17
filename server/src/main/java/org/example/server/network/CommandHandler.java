package org.example.server.network;

import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.controller.BidController;
import org.example.server.controller.ProductController;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.server.repository.ProductDao;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.product.ProductService;
import org.example.server.service.bid.BidService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles requests from clients and routes them to appropriate controllers.
 */
public class CommandHandler implements Runnable {
    private final SocketChannel clientChannel;
    private final String message;

    public CommandHandler(SocketChannel clientChannel, String message) {
        this.clientChannel = clientChannel;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Request request = JsonConverter.fromJson(message, Request.class);
            if (request == null) return;
            
            Response<?> response = handleRequest(request);
            sendResponse(response);
            
            // Special Broadcast Logic for Successful Bids
            if (request.getType() == MessageType.BID_PLACE && response.isSuccess()) {
                broadcastBidUpdate(request.getPayload());
            }
            
        } catch (Exception e) {
            FileLogger.error("Error handling command", e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error", null));
        }
    }

    private Response<?> handleRequest(Request request) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Instantiate Repositories
            UserDao userDao = new UserDao(conn);
            ProductDao productDao = new ProductDao(conn);
            
            // Instantiate Services
            AuthService authService = new AuthService(userDao);
            ProductService productService = new ProductService(productDao);
            BidService bidService = new BidService(conn); // Manages its own transaction logic

            // Instantiate Controllers
            AuthController authController = new AuthController(authService);
            ProductController productController = new ProductController(productService);
            BidController bidController = new BidController(bidService);

            switch (request.getType()) {
                case LOGIN:
                    return authController.handleLogin(request.getPayload());
                case SIGNUP:
                    return authController.handleSignup(request.getPayload());
                case PRODUCT_LIST:
                    return productController.handleGetAllAuctions();
                case PRODUCT_DETAIL:
                    return productController.handleGetAuctionDetail(request.getPayload());
                case BID_PLACE:
                    return bidController.handlePlaceBid(request.getPayload());
                default:
                    return new Response<>(MessageType.ERROR, false, "Unknown Command", null);
            }
        } catch (SQLException e) {
            FileLogger.error("Database error", e);
            return new Response<>(MessageType.ERROR, false, "Database Error", null);
        }
    }

    private void broadcastBidUpdate(Object payload) {
        // Triggered on successful bid to notify ALL connected clients
        Response<Object> update = new Response<>(MessageType.BID_UPDATE, true, "New highest bid!", payload);
        Broadcaster.broadcast(update);
    }

    private void sendResponse(Response<?> response) {
        try {
            String json = JsonConverter.toJson(response) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
        } catch (IOException e) {
            FileLogger.error("Failed to send response", e);
        }
    }
}
