package org.example.server.network.command;

import org.example.dto.request.BidHistoryRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve the paged bid history of a specific auction.
 */
public class BidHistoryCommand implements Command {
    private final BidController bidController;

    /**
     * Constructs a BidHistoryCommand with the specified BidController.
     *
     * @param bidController the controller for bidding operations
     */
    public BidHistoryCommand(BidController bidController) {
        this.bidController = bidController;
    }

    /**
     * Executes the bid history command.
     *
     * @param request the request containing the auction ID and pagination info
     * @param channel the socket channel of the client
     * @return the response containing paged bid history
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        BidHistoryRequest historyRequest = JsonConverter.convert(request.getPayload(), BidHistoryRequest.class);
        return bidController.handleGetBidHistory(historyRequest);
    }
}
