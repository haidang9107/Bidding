package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for a user to withdraw funds from their account.
 */
@RequiresRole(UserRole.MEMBER)
public class WithdrawCommand implements Command {
    private final FinanceController financeController;

    /**
     * Constructs a WithdrawCommand with the specified FinanceController.
     *
     * @param financeController the controller for financial operations
     */
    public WithdrawCommand(FinanceController financeController) {
        this.financeController = financeController;
    }

    /**
     * Executes the withdraw command.
     *
     * @param request the request containing the withdrawal amount
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the withdrawal
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        Long amount = JsonConverter.convert(request.getPayload(), Long.class);
        return financeController.handleWithdraw(user.getAccountname(), amount);
    }
}
