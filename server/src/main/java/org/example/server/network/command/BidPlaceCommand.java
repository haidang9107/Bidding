package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.dto.BidRequest;
import org.example.dto.BidResult;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.Broadcaster;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;
import java.util.Map;

public class BidPlaceCommand implements Command {
    private final BidController bidController;

    public BidPlaceCommand(BidController bidController) {
        this.bidController = bidController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        Response<?> response = bidController.handlePlaceBid(request.getPayload(), currentUser.getAccountname());
        if (response.isSuccess()) {
            BidRequest bidRequest = JsonConverter.fromJson(
                    JsonConverter.toJson(request.getPayload()), BidRequest.class);
            if (bidRequest != null && bidRequest.getAuctionId() > 0) {
                BidResult result = response.getData() instanceof BidResult bidResult ? bidResult : null;
                long amount = result == null ? bidRequest.getAmount() : result.getCurrentPrice();
                String winner = result == null ? currentUser.getAccountname() : result.getWinnerAccountname();
                boolean autoBidApplied = result != null && result.isAutoBidApplied();
                Broadcaster.broadcastToAuction(
                        bidRequest.getAuctionId(),
                        new Response<>(
                                MessageType.BID_UPDATE,
                                true,
                                "New highest bid",
                                Map.of(
                                        "auctionId", bidRequest.getAuctionId(),
                                        "bidderAccountname", winner,
                                        "amount", amount,
                                        "autoBidApplied", autoBidApplied
                                )
                        )
                );
            }
        }
        return response;
    }
}
