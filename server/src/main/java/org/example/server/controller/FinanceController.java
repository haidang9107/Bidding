package org.example.server.controller;

import org.example.dto.response.BalanceResponse;
import org.example.dto.request.TransferRequest;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;

/**
 * Controller for handling financial operations.
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

    public Response<BalanceResponse> handleDeposit(String accountname, Long amount) {
        BalanceResponse result = depositService.deposit(accountname, amount);
        return new Response<>(MessageType.DEPOSIT, true, "Deposit successful", result);
    }

    public Response<BalanceResponse> handleWithdraw(String accountname, Long amount) {
        BalanceResponse result = withdrawService.withdraw(accountname, amount);
        return new Response<>(MessageType.WITHDRAW, true, "Withdrawal successful", result);
    }

    public Response<BalanceResponse> handleTransfer(String fromAccount, TransferRequest transferReq) {
        BalanceResponse result = transferService.transfer(fromAccount, transferReq.getToAccountname(), transferReq.getAmount());
        return new Response<>(MessageType.TRANSFER, true, "Transfer successful", result);
    }
}
