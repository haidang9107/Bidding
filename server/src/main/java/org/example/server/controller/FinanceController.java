package org.example.server.controller;

import org.example.dto.TransferRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

/**
 * Controller for handling financial operations (Deposit, Withdraw, Transfer).
 */
public class FinanceController {
    private final DepositService depositService;
    private final WithdrawService withdrawService;
    private final TransferService transferService;

    public FinanceController(DepositService depositService, WithdrawService withdrawService, TransferService transferService) {
        this.depositService = depositService;
        this.withdrawService = withdrawService;
        this.transferService = transferService;
    }

    public Response<String> handleDeposit(String accountname, Object payload) {
        try {
            long amount = parseAmount(payload);
            String result = depositService.deposit(accountname, amount);
            return new Response<>(MessageType.DEPOSIT, "SUCCESS".equals(result), result, null);
        } catch (Exception e) {
            FileLogger.error("Finance: Deposit error", e);
            return new Response<>(MessageType.ERROR, false, "Invalid deposit amount format", null);
        }
    }

    public Response<String> handleWithdraw(String accountname, Object payload) {
        try {
            long amount = parseAmount(payload);
            String result = withdrawService.withdraw(accountname, amount);
            return new Response<>(MessageType.WITHDRAW, "SUCCESS".equals(result), result, null);
        } catch (Exception e) {
            FileLogger.error("Finance: Withdrawal error", e);
            return new Response<>(MessageType.ERROR, false, "Invalid withdrawal format", null);
        }
    }

    public Response<String> handleTransfer(String fromAccount, Object payload) {
        try {
            TransferRequest transferReq = JsonConverter.fromJson(JsonConverter.toJson(payload), TransferRequest.class);
            if (transferReq == null) {
                return new Response<>(MessageType.ERROR, false, "Invalid transfer request", null);
            }
            String result = transferService.transfer(fromAccount, transferReq.getToAccountname(), transferReq.getAmount());
            return new Response<>(MessageType.TRANSFER, "SUCCESS".equals(result), result, null);
        } catch (Exception e) {
            FileLogger.error("Finance: Transfer error", e);
            return new Response<>(MessageType.ERROR, false, "Internal error in transfer", null);
        }
    }

    private long parseAmount(Object payload) {
        if (payload instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(payload.toString());
    }
}
