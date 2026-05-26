package org.example.server.service.finance;

import org.example.dto.response.BalanceResponse;
import org.example.model.user.Member;
import org.example.server.exception.FinanceException;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private TransactionManager txManager;
    @Mock private UserDao            userDao;
    @Mock private TransactionDao     transactionDao;
    @Mock private Connection         connection;

    // ── setup helpers ─────────────────────────────────────────────────────

    private void wireTxExecute(TransactionManager tm) throws Exception {
        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(tm).execute(any());
    }

    private void wireTxRun(TransactionManager tm) throws Exception {
        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalRunnable fn = inv.getArgument(0);
            fn.execute(connection);
            return null;
        }).when(tm).run(any());
    }

    private Member member(String acct, long balance, long blocked) {
        return new Member(acct, "h", acct + "@t.com", null, 0, balance, blocked);
    }

    private static void setField(Object obj, String name, Object val) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DepositService
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DepositService")
    class DepositServiceTests {

        private DepositService depositService;

        @BeforeEach
        void setUp() throws Exception {
            depositService = new DepositService(txManager);
            setField(depositService, "userDao",          userDao);
            setField(depositService, "transactionDao",   transactionDao);
            wireTxExecute(txManager);
        }

        @Test
        @DisplayName("TC-DEP-01: nạp tiền thành công – trả về balance mới")
        void deposit_success() throws Exception {
            Member m = member("alice", 1_000L, 0L);
            when(userDao.addBalance(connection, "alice", 500L)).thenReturn(true);
            // Sau khi addBalance, findByAccountname trả về balance đã cộng
            Member updated = member("alice", 1_500L, 0L);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(updated);

            BalanceResponse resp = depositService.deposit("alice", 500L);

            assertEquals(1_500L, resp.getNewBalance());
            verify(transactionDao).insertTransaction(any(), isNull(), eq("alice"), any(), isNull(), eq(500L), isNull(), anyString());
        }

        @Test
        @DisplayName("TC-DEP-02: amount <= 0 ném FinanceException trước khi DB")
        void deposit_zeroAmount_throws() {
            assertThrows(FinanceException.class, () -> depositService.deposit("alice", 0L));
            verifyNoInteractions(userDao);
        }

        @Test
        @DisplayName("TC-DEP-03: amount âm ném FinanceException")
        void deposit_negativeAmount_throws() {
            assertThrows(FinanceException.class, () -> depositService.deposit("alice", -100L));
        }

        @Test
        @DisplayName("TC-DEP-04: user không tồn tại (addBalance false) ném FinanceException")
        void deposit_userNotFound_throws() throws Exception {
            when(userDao.addBalance(connection, "ghost", 100L)).thenReturn(false);
            assertThrows(FinanceException.class, () -> depositService.deposit("ghost", 100L));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  WithdrawService
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("WithdrawService")
    class WithdrawServiceTests {

        private WithdrawService withdrawService;

        @BeforeEach
        void setUp() throws Exception {
            withdrawService = new WithdrawService(txManager);
            setField(withdrawService, "userDao",        userDao);
            setField(withdrawService, "transactionDao", transactionDao);
            wireTxExecute(txManager);
        }

        @Test
        @DisplayName("TC-WDR-01: rút tiền thành công")
        void withdraw_success() throws Exception {
            Member m = member("alice", 2_000L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);
            when(userDao.addBalance(connection, "alice", -500L)).thenReturn(true);

            BalanceResponse resp = withdrawService.withdraw("alice", 500L);

            assertEquals(1_500L, resp.getNewBalance());
        }

        @Test
        @DisplayName("TC-WDR-02: amount <= 0 ném FinanceException")
        void withdraw_zeroAmount_throws() {
            assertThrows(FinanceException.class, () -> withdrawService.withdraw("alice", 0L));
        }

        @Test
        @DisplayName("TC-WDR-03: số dư không đủ (available < amount) ném FinanceException")
        void withdraw_insufficientFunds_throws() throws Exception {
            Member m = member("alice", 300L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(FinanceException.class, () -> withdrawService.withdraw("alice", 500L));
        }

        @Test
        @DisplayName("TC-WDR-04: available = balance - blockedBalance, blocked không được rút")
        void withdraw_blockedAmountNotAvailable() throws Exception {
            // balance=1000, blocked=800 → available=200
            Member m = member("alice", 1_000L, 800L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(FinanceException.class, () -> withdrawService.withdraw("alice", 500L));
        }

        @Test
        @DisplayName("TC-WDR-05: user không tồn tại ném FinanceException")
        void withdraw_userNotFound_throws() throws Exception {
            when(userDao.findByAccountnameForUpdate(connection, "ghost")).thenReturn(null);
            assertThrows(FinanceException.class, () -> withdrawService.withdraw("ghost", 100L));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TransferService
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TransferService")
    class TransferServiceTests {

        private TransferService transferService;

        @BeforeEach
        void setUp() throws Exception {
            transferService = new TransferService(txManager);
            setField(transferService, "userDao",        userDao);
            setField(transferService, "transactionDao", transactionDao);
            wireTxExecute(txManager);
        }

        @Test
        @DisplayName("TC-TRF-01: chuyển tiền thành công")
        void transfer_success() throws Exception {
            Member from    = member("alice", 2_000L, 0L);
            Member to      = member("bob",   1_000L, 0L);
            Member updated = member("alice", 1_500L, 0L);

            // lock order: alice < bob alphabetically
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountnameForUpdate(connection, "bob")).thenReturn(to);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(from).thenReturn(updated);
            when(userDao.findByAccountname(connection, "bob")).thenReturn(to);
            when(userDao.addBalance(connection, "alice", -500L)).thenReturn(true);
            when(userDao.addBalance(connection, "bob",    500L)).thenReturn(true);

            BalanceResponse resp = transferService.transfer("alice", "bob", 500L);
            assertEquals(1_500L, resp.getNewBalance());
        }

        @Test
        @DisplayName("TC-TRF-02: amount <= 0 ném FinanceException")
        void transfer_zeroAmount_throws() {
            assertThrows(FinanceException.class,
                    () -> transferService.transfer("alice", "bob", 0L));
        }

        @Test
        @DisplayName("TC-TRF-03: chuyển cho chính mình ném FinanceException")
        void transfer_selfTransfer_throws() {
            assertThrows(FinanceException.class,
                    () -> transferService.transfer("alice", "alice", 100L));
        }

        @Test
        @DisplayName("TC-TRF-04: sender không đủ tiền ném FinanceException")
        void transfer_insufficientFunds_throws() throws Exception {
            Member from = member("alice", 100L, 0L);
            Member to   = member("bob",   500L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountnameForUpdate(connection, "bob")).thenReturn(to);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountname(connection, "bob")).thenReturn(to);

            assertThrows(FinanceException.class,
                    () -> transferService.transfer("alice", "bob", 500L));
        }

        @Test
        @DisplayName("TC-TRF-05: recipient không tồn tại ném FinanceException")
        void transfer_recipientNotFound_throws() throws Exception {
            Member from = member("alice", 2_000L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountnameForUpdate(connection, "bob")).thenReturn(null);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountname(connection, "bob")).thenReturn(null);

            assertThrows(FinanceException.class,
                    () -> transferService.transfer("alice", "bob", 100L));
        }

        @Test
        @DisplayName("TC-TRF-06: khóa theo thứ tự alphabet tránh deadlock (z > a)")
        void transfer_lockOrderRespected() throws Exception {
            // "zara" > "alice" → lock alice trước
            Member from    = member("zara",  2_000L, 0L);
            Member to      = member("alice", 1_000L, 0L);
            Member updated = member("zara",  1_500L, 0L);

            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(to);
            when(userDao.findByAccountnameForUpdate(connection, "zara")).thenReturn(from);
            when(userDao.findByAccountname(connection, "zara")).thenReturn(from).thenReturn(updated);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(to);
            when(userDao.addBalance(connection, "zara",  -500L)).thenReturn(true);
            when(userDao.addBalance(connection, "alice",  500L)).thenReturn(true);

            BalanceResponse resp = transferService.transfer("zara", "alice", 500L);
            assertEquals(1_500L, resp.getNewBalance());

            // Verify alice was locked first (lock-order: alice < zara)
            var inOrder = inOrder(userDao);
            inOrder.verify(userDao).findByAccountnameForUpdate(connection, "alice");
            inOrder.verify(userDao).findByAccountnameForUpdate(connection, "zara");
        }
    }
}
