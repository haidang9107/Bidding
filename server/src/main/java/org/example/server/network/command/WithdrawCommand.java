package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.network.SessionManager;
import org.example.server.service.finance.WithdrawService;
import org.example.util.FileLogger;

import java.nio.channels.SocketChannel;

public class WithdrawCommand implements Command {
    private final WithdrawService withdrawService;

    public WithdrawCommand(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        try {
            long amount;
            Object payload = request.getPayload();
            if (payload instanceof Number number) {
                amount = number.longValue();
            } else {
                amount = Long.parseLong(payload.toString());
            }

            String result = withdrawService.withdraw(user.getAccountname(), amount);
            return new Response<>(MessageType.WITHDRAW, result.equals("SUCCESS"), result, null);
        } catch (Exception e) {
            FileLogger.error("Withdrawal parsing error", e);
            return new Response<>(MessageType.ERROR, false, "Invalid withdrawal format", null);
        }
    }
}
