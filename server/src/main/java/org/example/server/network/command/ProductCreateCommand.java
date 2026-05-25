package org.example.server.network.command;

import org.example.dto.request.ProductCreateRequest;
import org.example.model.enums.MessageType;
import org.example.model.product.ItemFactory;
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

        // If an image was uploaded as Base64, persist it to disk and replace
        // the in-memory blob with the resulting file URL.
        String imageUrl = saveUploadedImageIfPresent(req, currentUser.getAccountname());

        // Use the factory to build the right subtype based on category metadata.
        Product product = ItemFactory.createProduct(
                req.getCategory(),
                JsonConverter.toJson(req)
        );
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setCategory(req.getCategory());
        product.setOwnerAccountname(currentUser.getAccountname());
        if (imageUrl != null) {
            product.setImageUrl(imageUrl);
        }

        int productId = productService.createInventoryProduct(product);
        FileLogger.info("Inventory product created: id=" + productId
                + " owner=" + currentUser.getAccountname());
        return new Response<>(MessageType.SUCCESS, true,
                "Đã thêm sản phẩm vào kho", productId);
    }

    private String saveUploadedImageIfPresent(ProductCreateRequest req, String sellerAccount) {
        String b64 = req.getImageBase64();
        if (b64 == null || b64.isEmpty()) return null;
        try {
            String pure = b64;
            String mimeFromDataUrl = null;
            int comma = pure.indexOf(',');
            if (pure.startsWith("data:") && comma > 0) {
                String header = pure.substring(5, comma);
                int semi = header.indexOf(';');
                mimeFromDataUrl = (semi > 0) ? header.substring(0, semi) : header;
                pure = pure.substring(comma + 1);
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(pure);

            String mime = req.getImageMimeType();
            if (mime == null || mime.isEmpty()) mime = mimeFromDataUrl;
            String ext = guessExtension(mime);

            java.nio.file.Path dir = java.nio.file.Paths.get("uploads", "products");
            java.nio.file.Files.createDirectories(dir);
            String filename = "p_" + sellerAccount + "_" + System.currentTimeMillis() + "." + ext;
            java.nio.file.Path target = dir.resolve(filename);
            java.nio.file.Files.write(target, bytes);

            req.setImageBase64(null); // clear so the blob isn't passed downstream
            return target.toUri().toString();
        } catch (Exception e) {
            FileLogger.warn("Failed to save uploaded product image: " + e.getMessage());
            return null;
        }
    }

    private String guessExtension(String mime) {
        if (mime == null) return "png";
        return switch (mime.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif"               -> "gif";
            case "image/webp"              -> "webp";
            case "image/bmp"               -> "bmp";
            default                        -> "png";
        };
    }
}
