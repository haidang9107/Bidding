package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;

import java.nio.channels.SocketChannel;

public class AutoBidCancelCommand implements Command {
    private final BidController bidController;

    public AutoBidCancelCommand(BidController bidController) {
        this.bidController = bidController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        return bidController.handleCancelAutoBid(request.getPayload(), currentUser.getAccountname());
    }
}
