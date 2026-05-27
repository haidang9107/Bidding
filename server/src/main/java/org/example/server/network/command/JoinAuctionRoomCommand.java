package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.RoomManager;
import org.example.server.service.auction.AuctionService;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to join a specific auction room to receive realtime updates.
 */
@RequiresRole(UserRole.MEMBER)
public class JoinAuctionRoomCommand implements Command {
    private final AuctionService auctionService;

    /**
     * Constructs a JoinAuctionRoomCommand.
     * @param auctionService The auction service used to verify auction existence.
     */
    public JoinAuctionRoomCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Executes the join auction room command.
     * @param request The request containing the auction ID.
     * @param channel The socket channel of the user.
     * @return The response indicating success or failure of joining the room.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest roomRequest = JsonConverter.fromJson(
                JsonConverter.toJson(request.getPayload()), AuctionRoomRequest.class);
        if (roomRequest == null || roomRequest.getAuctionId() <= 0) {
            return new Response<>(MessageType.ERROR, false, "auctionId is required", null);
        }

        try {
            if (auctionService.getAuctionById(roomRequest.getAuctionId()) == null) {
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
