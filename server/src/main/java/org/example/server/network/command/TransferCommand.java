package org.example.server.network.command;

import org.example.dto.request.TransferRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to transfer funds to another user.
 */
public class TransferCommand implements Command {
    private final FinanceController financeController;

    /**
     * Constructs a TransferCommand with the specified FinanceController.
     *
     * @param financeController the controller for financial operations
     */
    public TransferCommand(FinanceController financeController) {
        this.financeController = financeController;
    }

    /**
     * Executes the transfer command.
     *
     * @param request the request containing TransferRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the transfer
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        TransferRequest transferReq = JsonConverter.convert(request.getPayload(), TransferRequest.class);
        return financeController.handleTransfer(user.getAccountname(), transferReq);
    }
}
