package org.example.server.controller;

import org.example.dto.PagedResponse;
import org.example.dto.PaginationRequest;
import org.example.dto.ProductResponse;
import org.example.model.product.Item;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.product.ProductService;
import java.util.List;
import java.util.stream.Collectors;

import org.example.util.FileLogger;

/**
 * Controller for handling product and auction session requests.
 */
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Handles retrieving a paginated list of products.
     */
    public Response<?> handleGetAllAuctions(Object payload) {
        PaginationRequest pagReq;
        if (payload == null) {
            pagReq = new PaginationRequest(1, 10); // Default
        } else {
            try {
                pagReq = org.example.util.JsonConverter.fromJson(
                    org.example.util.JsonConverter.toJson(payload), PaginationRequest.class);
                if (pagReq == null) pagReq = new PaginationRequest(1, 10);
            } catch (Exception e) {
                pagReq = new PaginationRequest(1, 10);
            }
        }

        PagedResponse<Item> pagedItems = productService.getAuctionsPaged(pagReq.getPage(), pagReq.getPageSize());
        
        List<ProductResponse> productResponses = pagedItems.getItems().stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());
        
        PagedResponse<ProductResponse> finalResponse = new PagedResponse<>(
            productResponses, 
            pagedItems.getTotalItems(), 
            pagedItems.getCurrentPage(), 
            pagedItems.getPageSize()
        );

        return new Response<>(MessageType.PRODUCT_LIST, true, "Auctions fetched successfully", finalResponse);
    }

    public Response<ProductResponse> handleGetAuctionDetail(Object payload) {
        if (payload == null) {
            return new Response<>(MessageType.ERROR, false, "Product ID required", null);
        }
        try {
            int productId = Integer.parseInt(payload.toString());
            Item item = productService.getAuctionById(productId);
            if (item != null) {
                return new Response<>(MessageType.PRODUCT_DETAIL, true, "Product found", new ProductResponse(item));
            } else {
                return new Response<>(MessageType.ERROR, false, "Product not found", null);
            }
        } catch (Exception e) {
            FileLogger.error("Failed to fetch product detail", e);
            return new Response<>(MessageType.ERROR, false, "Invalid Product ID or internal error", null);
        }
    }

    public Response<String> handleCreateAuction(Object payload, String sellerAccount) {
        try {
            org.example.dto.ProductAddRequest addReq = org.example.util.JsonConverter.fromJson(
                org.example.util.JsonConverter.toJson(payload), org.example.dto.ProductAddRequest.class);
            
            if (addReq == null) {
                return new Response<>(MessageType.ERROR, false, "Invalid product data", null);
            }

            boolean success = productService.createAuction(addReq, sellerAccount);
            if (success) {
                return new Response<>(MessageType.SUCCESS, true, "Product added successfully", null);
            } else {
                return new Response<>(MessageType.ERROR, false, "Failed to create auction", null);
            }
        } catch (Exception e) {
            FileLogger.error("Error in handleCreateAuction", e);
            return new Response<>(MessageType.ERROR, false, "Internal error creating auction", null);
        }
    }
}
