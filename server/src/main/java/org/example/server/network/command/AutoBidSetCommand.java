package org.example.server.network.command;

import org.example.dto.AutoBidRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.network.Broadcaster;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;
import java.util.Map;

public class AutoBidSetCommand implements Command {
    private final BidController bidController;

    public AutoBidSetCommand(BidController bidController) {
        this.bidController = bidController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        AutoBidRequest autoBidRequest = JsonConverter.convert(request.getPayload(), AutoBidRequest.class);
        Response<?> response = bidController.handleConfigureAutoBid(
                autoBidRequest, currentUser.getAccountname());
                
        if (response.isSuccess() && autoBidRequest != null && autoBidRequest.getAuctionId() > 0) {
            Broadcaster.broadcastToAuction(
                    autoBidRequest.getAuctionId(),
                    new Response<>(
                            MessageType.NOTIFICATION,
                            true,
                            "Auto bid configured",
                            Map.of(
                                    "auctionId", autoBidRequest.getAuctionId(),
                                    "bidderAccountname", currentUser.getAccountname()
                            )
                    )
            );
        }
        return response;
    }
}
