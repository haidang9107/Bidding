package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class DepositCommand implements Command {
    private final FinanceController financeController;

    public DepositCommand(FinanceController financeController) {
        this.financeController = financeController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        
        Long amount = JsonConverter.convert(request.getPayload(), Long.class);
        return financeController.handleDeposit(user.getAccountname(), amount);
    }
}
