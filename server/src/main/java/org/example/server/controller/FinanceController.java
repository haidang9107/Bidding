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

    /**
     * Constructs a FinanceController with the specified services.
     *
     * @param depositService the service for deposits
     * @param withdrawService the service for withdrawals
     * @param transferService the service for transfers
     */
    public FinanceController(DepositService depositService, WithdrawService withdrawService, TransferService transferService) {
        this.depositService = depositService;
        this.withdrawService = withdrawService;
        this.transferService = transferService;
    }

    /**
     * Handles a deposit request.
     *
     * @param accountname the account name to deposit into
     * @param amount the amount to deposit
     * @return a response containing the new balance
     */
    public Response<BalanceResponse> handleDeposit(String accountname, Long amount) {
        BalanceResponse result = depositService.deposit(accountname, amount);
        return new Response<>(MessageType.DEPOSIT, true, "Deposit successful", result);
    }

    /**
     * Handles a withdrawal request.
     *
     * @param accountname the account name to withdraw from
     * @param amount the amount to withdraw
     * @return a response containing the new balance
     */
    public Response<BalanceResponse> handleWithdraw(String accountname, Long amount) {
        BalanceResponse result = withdrawService.withdraw(accountname, amount);
        return new Response<>(MessageType.WITHDRAW, true, "Withdrawal successful", result);
    }

    /**
     * Handles a transfer request between accounts.
     * @param fromAccount The sender's account name.
     * @param transferReq The transfer request details.
     * @return A response containing the sender's new balance.
     */
    public Response<BalanceResponse> handleTransfer(String fromAccount, TransferRequest transferReq) {
        BalanceResponse result = transferService.transfer(fromAccount, transferReq.getToAccountname(), transferReq.getAmount());
        return new Response<>(MessageType.TRANSFER, true, "Transfer successful", result);
    }
}
