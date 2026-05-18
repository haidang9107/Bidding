package org.example.server.network;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
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
                String[] data = request.getPayload().toString().split(":");
                int productId = Integer.parseInt(data[0]);
                broadcastBidUpdate(productId);
            }
            
        } catch (Exception e) {
            FileLogger.error("Error handling command", e);
            sendResponse(new Response<>(MessageType.ERROR, false, "Internal Server Error", null));
        }
    }

    private Response<?> handleRequest(Request request) {
        User currentUser = SessionManager.getUser(clientChannel);

        // 1. Authentication Check (Protects all routes except LOGIN and SIGNUP)
        if (request.getType() != MessageType.LOGIN && request.getType() != MessageType.SIGNUP) {
            if (currentUser == null) {
                return new Response<>(MessageType.ERROR, false, "Unauthorized: Please login first", null);
            }
        }

        // 2. Authorization Check (Role-based)
        if (!hasPermission(request.getType(), currentUser)) {
            return new Response<>(MessageType.ERROR, false, "Forbidden: You don't have permission to perform this action", null);
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Instantiate Repositories
            UserDao userDao = new UserDao(conn);
            ProductDao productDao = new ProductDao(conn);
            
            // Instantiate Services
            AuthService authService = new AuthService(userDao);
            ProductService productService = new ProductService(productDao);
            BidService bidService = new BidService(conn);

            // Instantiate Controllers
            AuthController authController = new AuthController(authService);
            ProductController productController = new ProductController(productService);
            BidController bidController = new BidController(bidService);

            switch (request.getType()) {
                case LOGIN:
                    Response<?> loginResponse = authController.handleLogin(request.getPayload());
                    if (loginResponse.isSuccess() && loginResponse.getData() instanceof User user) {
                        SessionManager.login(clientChannel, user);
                    }
                    return loginResponse;

                case SIGNUP:
                    return authController.handleSignup(request.getPayload());
                
                case LOGOUT:
                    SessionManager.logout(clientChannel);
                    return new Response<>(MessageType.SUCCESS, true, "Logout successful", null);
                
                case GET_PROFILE:
                case UPDATE_PROFILE:
                    return new Response<>(MessageType.ERROR, false, "Profile features are coming soon!", null);
                
                case PRODUCT_LIST:
                    return productController.handleGetAllAuctions();
                case PRODUCT_DETAIL:
                    return productController.handleGetAuctionDetail(request.getPayload());
                case PRODUCT_ADD:
                    return new Response<>(MessageType.ERROR, false, "Adding products via socket is not yet implemented", null);
                
                case BID_PLACE:
                    return bidController.handlePlaceBid(request.getPayload());
                
                default:
                    return new Response<>(MessageType.ERROR, false, "Unknown Command: " + request.getType(), null);
            }
        } catch (SQLException e) {
            FileLogger.error("Database error", e);
            return new Response<>(MessageType.ERROR, false, "Database Error", null);
        }
    }

    /**
     * Helper to verify if a user has permission for a specific message type.
     */
    private boolean hasPermission(MessageType type, User user) {
        // Public routes
        if (type == MessageType.LOGIN || type == MessageType.SIGNUP) return true;
        
        // If user is null but it's not a public route, it would have been caught by Auth Check,
        // but adding this for safety.
        if (user == null) return false;

        // RBAC Logic
        return switch (type) {
            case PRODUCT_ADD -> user.getRole() == org.example.model.enums.UserRole.ADMIN;
            // Add more specific role checks here
            // case DELETE_USER -> user.getRole() == UserRole.ADMIN;
            default -> true; // By default, logged in users can access other routes
        };
    }

    private void broadcastBidUpdate(int productId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            ProductDao productDao = new ProductDao(conn);
            Object product = productDao.getProductById(productId);
            Response<Object> update = new Response<>(MessageType.BID_UPDATE, true, "New highest bid!", product);
            Broadcaster.broadcast(update);
        } catch (SQLException e) {
            FileLogger.error("Failed to broadcast bid update", e);
        }
    }

    private void sendResponse(Response<?> response) {
        synchronized (clientChannel) {
            try {
                String json = JsonConverter.toJson(response) + "\n";
                ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) {
                    int written = clientChannel.write(buffer);
                    if (written == 0) {
                        Thread.onSpinWait();
                    }
                }
            } catch (IOException e) {
                FileLogger.error("Failed to send response", e);
            }
        }
    }
}
