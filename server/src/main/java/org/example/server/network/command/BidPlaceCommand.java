package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.dto.request.BidRequest;
import org.example.dto.response.BidResult;
import org.example.dto.notify.BidUpdateNotify;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.Broadcaster;
import org.example.server.controller.BidController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;
import java.sql.Timestamp;

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

        BidRequest bidRequest = JsonConverter.convert(request.getPayload(), BidRequest.class);
        Response<?> response = bidController.handlePlaceBid(bidRequest, currentUser.getAccountname());

        if (response.isSuccess() && bidRequest != null) {
            BidResult result = response.getData() instanceof BidResult bidResult ? bidResult : null;
            long amount = result == null ? bidRequest.getAmount() : result.getCurrentPrice();
            String winner = result == null ? currentUser.getAccountname() : result.getWinnerAccountname();
            boolean autoBidApplied = result != null && result.isAutoBidApplied();
            Timestamp newEndTime = result == null ? null : (Timestamp) result.getNewEndTime();

            Broadcaster.broadcastToAuction(
                    bidRequest.getAuctionId(),
                    new Response<>(
                            MessageType.BID_UPDATE,
                            true,
                            "New highest bid",
                            new BidUpdateNotify(
                                    bidRequest.getAuctionId(),
                                    winner,
                                    amount,
                                    autoBidApplied,
                                    newEndTime
                            )
                    )
            );
        }
        return response;
    }
}
