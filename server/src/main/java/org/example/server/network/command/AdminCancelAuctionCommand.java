package org.example.server.network.command;

import org.example.dto.request.AuctionCancelRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for an administrator to cancel an active or scheduled auction.
 */
public class AdminCancelAuctionCommand implements Command {
    private final AdminController adminController;

    /**
     * Constructs an AdminCancelAuctionCommand with the specified AdminController.
     *
     * @param adminController the controller for administrative operations
     */
    public AdminCancelAuctionCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    /**
     * Executes the auction cancellation command.
     *
     * @param request the request containing AuctionCancelRequest
     * @param channel the socket channel of the administrator
     * @return the response from the admin controller
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AuctionCancelRequest cancelReq = JsonConverter.convert(request.getPayload(), AuctionCancelRequest.class);
        return adminController.handleCancelAuction(cancelReq);
    }
}
