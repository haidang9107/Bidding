package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.RoomManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to leave an auction room and stop receiving its updates.
 */
public class LeaveAuctionRoomCommand implements Command {
    /**
     * Executes the leave auction room command.
     *
     * @param request the request containing AuctionRoomRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of leaving the room
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionRoomRequest roomRequest = JsonConverter.fromJson(
                JsonConverter.toJson(request.getPayload()), AuctionRoomRequest.class);
        if (roomRequest == null || roomRequest.getAuctionId() <= 0) {
            return new Response<>(MessageType.ERROR, false, "auctionId is required", null);
        }

        RoomManager.leaveAuction(roomRequest.getAuctionId(), channel);
        return new Response<>(MessageType.SUCCESS, true,
                "Left auction room " + roomRequest.getAuctionId(), null);
    }
}
