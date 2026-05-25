package org.example.server.controller;

import org.example.dto.request.PaginationRequest;
import org.example.dto.request.ProductAddRequest;
import org.example.dto.response.PagedResponse;
import org.example.dto.response.ProductResponse;
import org.example.model.Auction;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.exception.NotFoundException;
import org.example.server.service.auction.AuctionService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling auction session requests (list, detail, create).
 */
public class AuctionController {
    private final AuctionService auctionService;

    /**
     * Constructs an AuctionController.
     * @param auctionService The auction service.
     */
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Handles retrieving a paginated list of auctions.
     * @param pagReq The pagination request.
     * @return A response containing the paged list of auctions (with product info).
     */
    public Response<?> handleGetAllAuctions(PaginationRequest pagReq) {
        if (pagReq == null) {
            pagReq = new PaginationRequest(1, 10);
        }

        PagedResponse<Auction> paged = auctionService.getAuctionsPaged(pagReq.getPage(), pagReq.getPageSize());

        List<ProductResponse> productResponses = paged.getItems().stream()
                .map(ProductResponse::new)
                .collect(Collectors.toList());

        PagedResponse<ProductResponse> finalResponse = new PagedResponse<>(
                productResponses,
                paged.getTotalItems(),
                paged.getCurrentPage(),
                paged.getPageSize()
        );

        return new Response<>(MessageType.PRODUCT_LIST, true, "Auctions fetched successfully", finalResponse);
    }

    /**
     * Handles retrieving the details of a specific auction.
     * @param auctionId The auction ID.
     * @return A response containing the auction details (with product info).
     * @throws NotFoundException if the auction is not found.
     */
    public Response<ProductResponse> handleGetAuctionDetail(Integer auctionId) {
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            throw new NotFoundException("Auction not found with ID: " + auctionId);
        }
        return new Response<>(MessageType.PRODUCT_DETAIL, true, "Auction found", new ProductResponse(auction));
    }

    /**
     * Handles the creation of a new auction (and its underlying product).
     * @param addReq        The request details.
     * @param sellerAccount The seller's account name.
     * @return A success response.
     */
    public Response<String> handleCreateAuction(ProductAddRequest addReq, String sellerAccount) {
        auctionService.createAuction(addReq, sellerAccount);
        return new Response<>(MessageType.SUCCESS, true, "Auction created successfully", null);
    }
}
