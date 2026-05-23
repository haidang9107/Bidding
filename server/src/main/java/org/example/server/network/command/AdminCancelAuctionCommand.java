package org.example.server.network.command;

import org.example.dto.request.AuctionCancelRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class AdminCancelAuctionCommand implements Command {
    private final AdminController adminController;

    public AdminCancelAuctionCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionCancelRequest cancelReq = JsonConverter.convert(request.getPayload(), AuctionCancelRequest.class);
        return adminController.handleCancelAuction(cancelReq);
    }
}
