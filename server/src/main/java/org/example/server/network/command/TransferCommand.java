package org.example.server.network.command;

import org.example.dto.TransferRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.service.finance.TransferService;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class TransferCommand implements Command {
    private final TransferService transferService;

    public TransferCommand(TransferService transferService) {
        this.transferService = transferService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        try {
            TransferRequest transferReq = JsonConverter.fromJson(JsonConverter.toJson(request.getPayload()), TransferRequest.class);
            if (transferReq == null) {
                return new Response<>(MessageType.ERROR, false, "Invalid transfer request", null);
            }
            String result = transferService.transfer(user.getAccountname(), transferReq.getToAccountname(), transferReq.getAmount());
            return new Response<>(MessageType.TRANSFER, result.equals("SUCCESS"), result, null);
        } catch (Exception e) {
            return new Response<>(MessageType.ERROR, false, "Internal error in transfer", null);
        }
    }
}
