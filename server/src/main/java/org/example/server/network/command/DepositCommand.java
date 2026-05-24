package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to deposit funds into their account.
 */
public class DepositCommand implements Command {
    private final FinanceController financeController;

    /**
     * Constructs a DepositCommand with the specified FinanceController.
     *
     * @param financeController the controller for financial operations
     */
    public DepositCommand(FinanceController financeController) {
        this.financeController = financeController;
    }

    /**
     * Executes the deposit command.
     *
     * @param request the request containing the deposit amount
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the deposit
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        Long amount = JsonConverter.convert(request.getPayload(), Long.class);
        return financeController.handleDeposit(user.getAccountname(), amount);
    }
}
