package org.example.server.service.finance;

import org.example.dto.response.BalanceResponse;
import org.example.model.user.Member;
import org.example.server.exception.FinanceException;
import org.example.server.repository.DatabaseConnectionPool;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    private TransactionManager txManager;
    @Mock private DatabaseConnectionPool pool;
    @Mock private UserDao            userDao;
    @Mock private TransactionDao     transactionDao;
    @Mock private Connection         connection;
    @Mock private EventPublisher     eventPublisher;

    @BeforeEach
    void globalSetUp() throws SQLException {
        // Use a real TransactionManager with a mocked pool/connection
        // This avoids Mockito 5's "Could not modify all classes" error on Java 25
        txManager = new TransactionManager(pool);
        org.mockito.Mockito.lenient().when(pool.getConnection()).thenReturn(connection);
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
            depositService = new DepositService(txManager, eventPublisher);
            setField(depositService, "userDao",          userDao);
            setField(depositService, "transactionDao",   transactionDao);
        }

        @Test
        @DisplayName("TC-DEP-01: nạp tiền thành công – trả về balance mới")
        void deposit_success() throws Exception {
            Member m = member("alice", 1_000L, 0L);
            when(userDao.addBalance(connection, "alice", 500L)).thenReturn(true);
            Member updated = member("alice", 1_500L, 0L);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(updated);

            BalanceResponse resp = depositService.deposit("alice", 500L);

            assertEquals(1_500L, resp.getNewBalance());
            verify(transactionDao).insertTransaction(any(), isNull(), eq("alice"), any(), isNull(), eq(500L), isNull(), anyString());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("TC-DEP-02: amount <= 0 ném FinanceException trước khi DB")
        void deposit_zeroAmount_throws() {
            assertThrows(FinanceException.class, () -> depositService.deposit("alice", 0L));
            verifyNoInteractions(userDao);
        }

        @Test
        @DisplayName("TC-DEP-03: user không tồn tại ném FinanceException")
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
            withdrawService = new WithdrawService(txManager, eventPublisher);
            setField(withdrawService, "userDao",        userDao);
            setField(withdrawService, "transactionDao", transactionDao);
        }

        @Test
        @DisplayName("TC-WDR-01: rút tiền thành công")
        void withdraw_success() throws Exception {
            Member m = member("alice", 2_000L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);
            when(userDao.addBalance(connection, "alice", -500L)).thenReturn(true);

            BalanceResponse resp = withdrawService.withdraw("alice", 500L);

            assertEquals(1_500L, resp.getNewBalance());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("TC-WDR-03: số dư không đủ ném FinanceException")
        void withdraw_insufficientFunds_throws() throws Exception {
            Member m = member("alice", 300L, 0L);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(FinanceException.class, () -> withdrawService.withdraw("alice", 500L));
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
            transferService = new TransferService(txManager, eventPublisher);
            setField(transferService, "userDao",        userDao);
            setField(transferService, "transactionDao", transactionDao);
        }

        @Test
        @DisplayName("TC-TRF-01: chuyển tiền thành công")
        void transfer_success() throws Exception {
            Member from    = member("alice", 2_000L, 0L);
            Member to      = member("bob",   1_000L, 0L);
            Member updated = member("alice", 1_500L, 0L);

            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(from);
            when(userDao.findByAccountnameForUpdate(connection, "bob")).thenReturn(to);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(from).thenReturn(updated);
            when(userDao.findByAccountname(connection, "bob")).thenReturn(to);
            when(userDao.addBalance(connection, "alice", -500L)).thenReturn(true);
            when(userDao.addBalance(connection, "bob",    500L)).thenReturn(true);

            BalanceResponse resp = transferService.transfer("alice", "bob", 500L);
            assertEquals(1_500L, resp.getNewBalance());
            org.mockito.Mockito.verify(eventPublisher, org.mockito.Mockito.times(2)).publish(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("TC-TRF-03: chuyển cho chính mình ném FinanceException")
        void transfer_selfTransfer_throws() {
            assertThrows(FinanceException.class,
                    () -> transferService.transfer("alice", "alice", 100L));
        }

        @Test
        @DisplayName("TC-TRF-06: khóa theo thứ tự alphabet")
        void transfer_lockOrderRespected() throws Exception {
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

            var inOrder = inOrder(userDao);
            inOrder.verify(userDao).findByAccountnameForUpdate(connection, "alice");
            inOrder.verify(userDao).findByAccountnameForUpdate(connection, "zara");
        }
    }
}
