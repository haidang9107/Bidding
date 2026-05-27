package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.ProductCreateRequest;
import org.example.model.enums.MessageType;
import org.example.model.product.Product;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.exception.ValidationException;
import org.example.server.network.SessionManager;
import org.example.server.service.product.ProductService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a seller to add a product to their own inventory WITHOUT
 * opening an auction. The seller can later put the product up for auction
 * via {@code AUCTION_OPEN}.
 */
@RequiresRole(UserRole.MEMBER)
public class ProductCreateCommand implements Command {

    private final ProductService productService;

    public ProductCreateCommand(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        ProductCreateRequest req = JsonConverter.convert(request.getPayload(), ProductCreateRequest.class);
        if (req == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid payload", null);
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ValidationException("Tên sản phẩm không được trống");
        }
        if (req.getCategory() == null) {
            throw new ValidationException("Loại sản phẩm không được trống");
        }

        // Use GSON's polymorphic deserialization to build the right subclass
        Product product = JsonConverter.fromJson(JsonConverter.toJson(req), Product.class);
        product.setOwnerAccountname(currentUser.getAccountname());

        int productId = productService.createInventoryProduct(product);
        FileLogger.info("Inventory product created: id=" + productId
                + " owner=" + currentUser.getAccountname());
        return new Response<>(MessageType.SUCCESS, true,
                "Đã thêm sản phẩm vào kho", productId);
    }
}
