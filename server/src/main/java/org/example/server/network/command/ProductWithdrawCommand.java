package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.annotation.RequiresRole;
import org.example.server.network.SessionManager;
import org.example.server.service.product.ProductService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;
import com.google.gson.JsonObject;

import java.nio.channels.SocketChannel;

/**
 * Command for a seller to withdraw (soft delete) an existing product from their inventory.
 * A product can only be withdrawn if it is not currently in an active auction.
 * Handles the {@link MessageType#PRODUCT_WITHDRAW} request.
 */
@RequiresRole(UserRole.MEMBER)
public class ProductWithdrawCommand implements Command {

    private final ProductService productService;

    /**
     * Constructs a ProductWithdrawCommand.
     * @param productService The service handling product withdrawal logic.
     */
    public ProductWithdrawCommand(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Executes the withdrawal request.
     * @param request The request containing the productId.
     * @param channel The user's socket channel.
     * @return A success or error response.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        JsonObject payload = JsonConverter.convert(request.getPayload(), JsonObject.class);
        if (payload == null || !payload.has("productId")) {
            return new Response<>(MessageType.ERROR, false, "Invalid payload: productId is required", null);
        }

        int productId = payload.get("productId").getAsInt();

        try {
            boolean success = productService.withdrawProduct(productId, currentUser.getAccountname());
            if (success) {
                FileLogger.info("Inventory product withdrawn: id=" + productId
                        + " owner=" + currentUser.getAccountname());
                return new Response<>(MessageType.SUCCESS, true, "Rút sản phẩm thành công", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Rút sản phẩm thất bại", null);
            }
        } catch (org.example.server.exception.BaseAppException e) {
            return new Response<>(MessageType.ERROR, false, e.getMessage(), null);
        } catch (Exception e) {
            FileLogger.error("Error withdrawing product", e);
            return new Response<>(MessageType.ERROR, false, "Internal server error", null);
        }
    }
}
