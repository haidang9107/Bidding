package org.example.server.network.command;

import org.example.dto.request.AuctionRoomRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.RoomManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class LeaveAuctionRoomCommand implements Command {
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
