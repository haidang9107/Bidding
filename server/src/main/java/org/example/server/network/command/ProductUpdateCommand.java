package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.ProductUpdateRequest;
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
 * Command for a seller to update an existing product in their inventory.
 * A product can only be updated if it is not currently in an active auction.
 */
@RequiresRole(UserRole.MEMBER)
public class ProductUpdateCommand implements Command {

    private final ProductService productService;

    public ProductUpdateCommand(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        ProductUpdateRequest req = JsonConverter.convert(request.getPayload(), ProductUpdateRequest.class);
        if (req == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid payload", null);
        }
        if (req.getProductId() <= 0) {
            throw new ValidationException("ID sản phẩm không hợp lệ");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ValidationException("Tên sản phẩm không được trống");
        }
        if (req.getCategory() == null) {
            throw new ValidationException("Loại sản phẩm không được trống");
        }

        // Verify product exists and belongs to the user
        Product existing = productService.getProductById(req.getProductId());
        if (existing == null) {
            return new Response<>(MessageType.ERROR, false, "Không tìm thấy sản phẩm", null);
        }
        if (!existing.getOwnerAccountname().equals(currentUser.getAccountname())) {
            return new Response<>(MessageType.ERROR, false, "Bạn không có quyền sửa sản phẩm này", null);
        }
        if (existing.isInAuction()) {
            return new Response<>(MessageType.ERROR, false, "Không thể sửa sản phẩm đang trong cuộc đấu giá", null);
        }

        // Use GSON's polymorphic deserialization via ProductFactory to build the right subclass
        Product product = JsonConverter.fromJson(JsonConverter.toJson(req), Product.class);
        
        // Ensure critical fields from existing data are preserved or correctly set
        product.setOwnerAccountname(currentUser.getAccountname());
        product.setInAuction(false);

        boolean success = productService.updateProduct(product);
        if (success) {
            FileLogger.info("Inventory product updated: id=" + product.getProductId()
                    + " owner=" + currentUser.getAccountname());
            return new Response<>(MessageType.SUCCESS, true, "Cập nhật sản phẩm thành công", null);
        } else {
            return new Response<>(MessageType.ERROR, false, "Cập nhật sản phẩm thất bại", null);
        }
    }
}
