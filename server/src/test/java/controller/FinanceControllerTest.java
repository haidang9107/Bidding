package controller;

import org.example.dto.request.PaginationRequest;
import org.example.dto.request.TransferRequest;
import org.example.dto.response.BalanceResponse;
import org.example.dto.response.PagedResponse;
import org.example.model.Transaction;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.controller.FinanceController;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.server.service.finance.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinanceControllerTest {

    private DepositService depositServiceMock;
    private WithdrawService withdrawServiceMock;
    private TransferService transferServiceMock;
    private TransactionService transactionServiceMock;
    private FinanceController financeController;

    @BeforeEach
    void setUp() {
        // Mock tất cả 4 tầng Service liên quan đến tài chính
        depositServiceMock = Mockito.mock(DepositService.class);
        withdrawServiceMock = Mockito.mock(WithdrawService.class);
        transferServiceMock = Mockito.mock(TransferService.class);
        transactionServiceMock = Mockito.mock(TransactionService.class);

        // Khởi tạo Controller truyền 4 Mock Service vào
        financeController = new FinanceController(
                depositServiceMock,
                withdrawServiceMock,
                transferServiceMock,
                transactionServiceMock
        );
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: handleDeposit (Nạp tiền)
    // ====================================================================
    @Test
    void testHandleDeposit_Success() {
        String account = "user_finance";
        Long amount = 100000L;

        BalanceResponse mockBalance = Mockito.mock(BalanceResponse.class);
        Mockito.when(depositServiceMock.deposit(account, amount)).thenReturn(mockBalance);

        Response<BalanceResponse> response = financeController.handleDeposit(account, amount);

        assertNotNull(response);
        assertEquals(MessageType.DEPOSIT, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Deposit successful", response.getMessage());
        assertEquals(mockBalance, response.getData());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handleWithdraw (Rút tiền)
    // ====================================================================
    @Test
    void testHandleWithdraw_Success() {
        String account = "user_finance";
        Long amount = 50000L;

        BalanceResponse mockBalance = Mockito.mock(BalanceResponse.class);
        Mockito.when(withdrawServiceMock.withdraw(account, amount)).thenReturn(mockBalance);

        Response<BalanceResponse> response = financeController.handleWithdraw(account, amount);

        assertNotNull(response);
        assertEquals(MessageType.WITHDRAW, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Withdrawal successful", response.getMessage());
        assertEquals(mockBalance, response.getData());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: handleTransfer (Chuyển tiền)
    // ====================================================================
    @Test
    void testHandleTransfer_Success() {
        String fromAccount = "sender_user";
        TransferRequest transferReq = new TransferRequest();
        transferReq.setToAccountname("receiver_user");
        transferReq.setAmount(30000L);

        BalanceResponse mockBalance = Mockito.mock(BalanceResponse.class);
        Mockito.when(transferServiceMock.transfer("sender_user", "receiver_user", 30000L)).thenReturn(mockBalance);

        Response<BalanceResponse> response = financeController.handleTransfer(fromAccount, transferReq);

        assertNotNull(response);
        assertEquals(MessageType.TRANSFER, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Transfer successful", response.getMessage());
        assertEquals(mockBalance, response.getData());
    }

    // ====================================================================
    // 4. TEST CASE CHO HÀM: handleGetTransactions (Lấy lịch sử - Paged)
    // ====================================================================
    @Test
    void testHandleGetTransactions_Success() {
        String account = "user_finance";
        PaginationRequest pagReq = new PaginationRequest(1, 10);

        Transaction tx1 = Mockito.mock(Transaction.class);
        Transaction tx2 = Mockito.mock(Transaction.class);
        PagedResponse<Transaction> mockPaged = new PagedResponse<>(Arrays.asList(tx1, tx2), 2, 1, 10);

        Mockito.when(transactionServiceMock.getTransactionsPaged(account, 1, 10)).thenReturn(mockPaged);

        Response<PagedResponse<Transaction>> response = financeController.handleGetTransactions(account, pagReq);

        assertNotNull(response);
        assertEquals(MessageType.TRANSACTION_HISTORY, response.getType());
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().getItems().size());
    }

    @Test
    void testHandleGetTransactions_NullRequest_Fallback() {
        String account = "user_finance";

        PagedResponse<Transaction> mockPaged = new PagedResponse<>(Arrays.asList(), 0, 1, 10);
        Mockito.when(transactionServiceMock.getTransactionsPaged(account, 1, 10)).thenReturn(mockPaged);

        // Truyền null request để check fallback page 1, size 10
        Response<PagedResponse<Transaction>> response = financeController.handleGetTransactions(account, null);

        assertNotNull(response);
        assertEquals(MessageType.TRANSACTION_HISTORY, response.getType());
        Mockito.verify(transactionServiceMock, Mockito.times(1)).getTransactionsPaged(account, 1, 10);
    }

    // ====================================================================
    // 5. TEST CASE CHO HÀM: handleGetTransactionsFull (Lấy full lịch sử)
    // ====================================================================
    @Test
    void testHandleGetTransactionsFull_Success() {
        String account = "user_finance";

        Transaction tx = Mockito.mock(Transaction.class);
        List<Transaction> mockList = Arrays.asList(tx);

        Mockito.when(transactionServiceMock.getTransactions(account)).thenReturn(mockList);

        Response<List<Transaction>> response = financeController.handleGetTransactionsFull(account);

        assertNotNull(response);
        assertEquals(MessageType.TRANSACTION_HISTORY, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Transactions retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
    }
}