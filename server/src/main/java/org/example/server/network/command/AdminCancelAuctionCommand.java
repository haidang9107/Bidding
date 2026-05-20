package org.example.server.network.command;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;

import java.nio.channels.SocketChannel;

public class AdminCancelAuctionCommand implements Command {
    private final AdminController adminController;

    public AdminCancelAuctionCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return adminController.handleCancelAuction(request.getPayload());
    }
}
