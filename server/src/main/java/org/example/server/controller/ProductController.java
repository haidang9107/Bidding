package org.example.server.controller;

import org.example.dto.response.PagedResponse;
import org.example.dto.request.PaginationRequest;
import org.example.dto.response.ProductResponse;
import org.example.dto.request.ProductAddRequest;
import org.example.model.product.Item;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.exception.NotFoundException;
import org.example.server.service.product.ProductService;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling product and auction session requests.
 */
public class ProductController {
    private final ProductService productService;

    /**
     * Constructs a ProductController with the specified ProductService.
     *
     * @param productService the product service to use for product operations
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Handles retrieving a paginated list of products.
     *
     * @param pagReq the pagination request details
     * @return a response containing the paged list of products
     */
    public Response<?> handleGetAllAuctions(PaginationRequest pagReq) {
        if (pagReq == null) {
            pagReq = new PaginationRequest(1, 10);
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

    /**
     * Handles retrieving the details of a specific auction.
     *
     * @param productId the ID of the product/auction
     * @return a response containing the product details
     * @throws NotFoundException if the product is not found
     */
    public Response<ProductResponse> handleGetAuctionDetail(Integer productId) {
        Item item = productService.getAuctionById(productId);
        if (item == null) {
            throw new NotFoundException("Product not found with ID: " + productId);
        }
        return new Response<>(MessageType.PRODUCT_DETAIL, true, "Product found", new ProductResponse(item));
    }

    /**
     * Handles the creation of a new auction.
     * @param addReq The request containing product details.
     * @param sellerAccount The account name of the seller.
     * @return A success response.
     */
    public Response<String> handleCreateAuction(ProductAddRequest addReq, String sellerAccount) {
        productService.createAuction(addReq, sellerAccount);
        return new Response<>(MessageType.SUCCESS, true, "Product added successfully", null);
    }
}
