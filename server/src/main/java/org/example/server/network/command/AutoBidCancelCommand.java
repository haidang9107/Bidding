package org.example.server.network.command;

import org.example.dto.AutoBidRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

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
        
        AutoBidRequest autoBidReq = JsonConverter.convert(request.getPayload(), AutoBidRequest.class);
        return bidController.handleCancelAutoBid(autoBidReq, currentUser.getAccountname());
    }
}
