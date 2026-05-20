package org.example.server.network.command;

import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.network.SessionManager;

import java.nio.channels.SocketChannel;

public class TransferCommand implements Command {
    private final FinanceController financeController;

    public TransferCommand(FinanceController financeController) {
        this.financeController = financeController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) return new Response<>(org.example.model.enums.MessageType.ERROR, false, "Unauthorized", null);
        return financeController.handleTransfer(user.getAccountname(), request.getPayload());
    }
}
