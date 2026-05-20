package org.example.server.controller;

import org.example.model.product.Item;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.product.ProductService;
import java.util.List;

/**
 * Controller for handling product and auction session requests.
 */
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public Response<List<Item>> handleGetAllAuctions() {
        List<Item> items = productService.getAllAuctions();
        return new Response<>(MessageType.PRODUCT_LIST, true, "Auctions fetched successfully", items);
    }

    public Response<Item> handleGetAuctionDetail(Object payload) {
        if (payload == null) {
            return new Response<>(MessageType.ERROR, false, "Product ID required", null);
        }
        try {
            int productId = Integer.parseInt(payload.toString());
            Item item = productService.getAuctionById(productId);
            if (item != null) {
                return new Response<>(MessageType.PRODUCT_DETAIL, true, "Product found", item);
            } else {
                return new Response<>(MessageType.ERROR, false, "Product not found", null);
            }
        } catch (NumberFormatException e) {
            return new Response<>(MessageType.ERROR, false, "Invalid Product ID format", null);
        }
    }

    public boolean handleCreateAuction(Item item) {
        return productService.createAuction(item);
    }
}
