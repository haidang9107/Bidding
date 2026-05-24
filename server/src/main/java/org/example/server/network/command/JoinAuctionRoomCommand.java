package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.RoomManager;
import org.example.server.service.product.ProductService;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to join a specific auction room to receive updates.
 */
public class JoinAuctionRoomCommand implements Command {
    private final ProductService productService;

    /**
     * Constructs a JoinAuctionRoomCommand with the specified ProductService.
     *
     * @param productService the service for product and auction data
     */
    public JoinAuctionRoomCommand(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Executes the join auction room command.
     *
     * @param request the request containing AuctionRoomRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of joining the room
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest roomRequest = JsonConverter.fromJson(
                JsonConverter.toJson(request.getPayload()), AuctionRoomRequest.class);
        if (roomRequest == null || roomRequest.getAuctionId() <= 0) {
            return new Response<>(MessageType.ERROR, false, "auctionId is required", null);
        }

        try {
            if (productService.getAuctionById(roomRequest.getAuctionId()) == null) {
                return new Response<>(MessageType.ERROR, false, "Auction not found", null);
            }
        } catch (Exception e) {
            return new Response<>(MessageType.ERROR, false, "Error validating auction", null);
        }

        RoomManager.joinAuction(roomRequest.getAuctionId(), channel);
        return new Response<>(MessageType.SUCCESS, true,
                "Joined auction room " + roomRequest.getAuctionId(), null);
    }
}
