package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.BidController;

import java.nio.channels.SocketChannel;

public class BidPlaceCommand implements Command {
    private final BidController bidController;

    public BidPlaceCommand(BidController bidController) {
        this.bidController = bidController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return bidController.handlePlaceBid(request.getPayload());
    }
}
