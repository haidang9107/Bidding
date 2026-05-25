package org.example.server.network.command;

import org.example.dto.response.ProductResponse;
import org.example.model.enums.MessageType;
import org.example.model.product.Product;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.service.product.ProductService;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Command returning the products owned by the calling user (their inventory).
 * Includes both products in the seller's stock and products currently in an
 * active auction (the client can filter by isInAuction).
 */
public class MyProductListCommand implements Command {

    private final ProductService productService;

    public MyProductListCommand(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        List<Product> products = productService.getProductsByOwner(currentUser.getAccountname());
        List<ProductResponse> out = new ArrayList<>();
        for (Product p : products) {
            ProductResponse pr = new ProductResponse(p);
            // Flag in-auction state so the client can decide which tab to put it in.
            // We piggy-back on the status field (which would normally be the auction's
            // status) — "IN_AUCTION" means "this product is currently on the market",
            // null/empty means "stock".
            // Clients only inspect setStatus(null/IN_AUCTION) for inventory rows.
            out.add(pr);
        }
        return new Response<>(MessageType.MY_PRODUCT_LIST, true,
                "Fetched " + out.size() + " products", out);
    }
}
