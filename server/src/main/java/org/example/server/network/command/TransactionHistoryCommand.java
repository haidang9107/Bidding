package org.example.server.network.command;

import org.example.dto.request.PaginationRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;
import org.example.model.user.User;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to retrieve transaction history.
 */
public class TransactionHistoryCommand implements Command {
    private final FinanceController controller;

    /**
     * Constructs a TransactionHistoryCommand.
     * @param controller The finance controller.
     */
    public TransactionHistoryCommand(FinanceController controller) {
        this.controller = controller;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel clientChannel) {
        User user = SessionManager.getUser(clientChannel);
        if (user == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        
        PaginationRequest pagReq = JsonConverter.convert(request.getPayload(), PaginationRequest.class);
        return controller.handleGetTransactions(user.getAccountname(), pagReq);
    }
}
