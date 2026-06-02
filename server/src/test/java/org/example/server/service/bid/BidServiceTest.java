package org.example.server.service.bid;

import org.example.dto.response.BidResult;
import org.example.model.Auction;
import org.example.model.AutoBid;
import org.example.model.Bid;
import org.example.dto.response.PagedResponse;
import org.example.model.enums.AuctionStatus;
import org.example.model.user.Member;
import org.example.server.event.EventPublisher;
import org.example.server.event.NewBidPlacedEvent;
import org.example.server.exception.FinanceException;
import org.example.server.exception.NotFoundException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.*;
import org.example.server.service.auction.AuctionMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BidService}.
 *
 * Strategy: mock all DAOs and the TransactionManager so every test runs
 * without a real database.  The TransactionManager stubs are configured
 * to immediately invoke the lambda they receive, forwarding a mock
 * {@link Connection}.
 */
@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    // ── mocks ──────────────────────────────────────────────────────────────
    @Mock private TransactionManager txManager;
    @Mock private EventPublisher     eventPublisher;
    @Mock private AuctionMonitor     auctionMonitor;
    @Mock private AuctionDao         auctionDao;
    @Mock private BidDao             bidDao;
    @Mock private UserDao            userDao;
    @Mock private AutoBidDao         autoBidDao;
    @Mock private Connection         connection;

    private BidService bidService;

    // ── helpers ────────────────────────────────────────────────────────────

    /** Builds a running auction whose end time is 1 hour from now. */
    private Auction runningAuction(int id, String seller, long currentPrice, long step) {
        Auction a = new Auction();
        a.setAuctionId(id);
        a.setSellerAccountname(seller);
        a.setCurrentPrice(currentPrice);
        a.setStepPrice(step);
        a.setStartingPrice(currentPrice);
        a.setStatus(AuctionStatus.RUNNING);
        a.setEndTime(new Timestamp(System.currentTimeMillis() + 3_600_000)); // +1h
        return a;
    }

    /** Builds a Member with given balance and blockedBalance. */
    private Member member(String account, long balance, long blocked) {
        Member m = new Member(account, "hashed", account + "@test.com", null, 0, balance, blocked);
        return m;
    }

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to inject mocked DAOs because BidService creates them
        // internally via `new`.  We replace the fields after construction.
        bidService = new BidService(txManager, eventPublisher, auctionMonitor);

        // Inject mocked DAOs via reflection
        setField(bidService, "auctionDao", auctionDao);
        setField(bidService, "bidDao",     bidDao);
        setField(bidService, "userDao",    userDao);
        setField(bidService, "autoBidDao", autoBidDao);

        // TransactionManager: make execute/run call the lambda immediately
        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(txManager).execute(any());

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalRunnable fn = inv.getArgument(0);
            fn.execute(connection);
            return null;
        }).when(txManager).run(any());

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(txManager).query(any());
    }

    // ── placeBid ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("placeBid()")
    class PlaceBid {

        @Test
        @DisplayName("TC-BID-01: đặt giá hợp lệ – trả về BidResult đúng")
        void validBid_returnsBidResult() throws Exception {
            Auction auction = runningAuction(1, "seller", 1_000L, 100L);
            Member  bidder  = member("alice", 5_000L, 0L);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(auction);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(bidder);
            when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

            BidResult result = bidService.placeBid(1, "alice", 1_100L);

            assertNotNull(result);
            assertEquals(1, result.getAuctionId());
            assertEquals("alice", result.getWinnerAccountname());
            assertEquals(1_100L, result.getCurrentPrice());
            verify(bidDao).insertBid(eq(connection), eq(1), eq("alice"), eq(1_100L), eq(false));
            verify(eventPublisher).publish(any(NewBidPlacedEvent.class));
        }

        @Test
        @DisplayName("TC-BID-02: auctionId <= 0 ném ValidationException")
        void invalidAuctionId_throwsValidation() {
            assertThrows(ValidationException.class,
                    () -> bidService.placeBid(0, "alice", 500L));
        }

        @Test
        @DisplayName("TC-BID-03: bidAmount <= 0 ném ValidationException")
        void negativeAmount_throwsValidation() {
            assertThrows(ValidationException.class,
                    () -> bidService.placeBid(1, "alice", -100L));
        }

        @Test
        @DisplayName("TC-BID-04: phiên đấu giá không tồn tại ném NotFoundException")
        void auctionNotFound_throwsNotFound() throws Exception {
            when(auctionDao.getAuctionForUpdate(connection, 99)).thenReturn(null);
            assertThrows(NotFoundException.class,
                    () -> bidService.placeBid(99, "alice", 500L));
        }

        @Test
        @DisplayName("TC-BID-05: phiên đã kết thúc (endTime quá khứ) ném NotFoundException")
        void expiredAuction_throwsNotFound() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            a.setEndTime(new Timestamp(System.currentTimeMillis() - 1_000)); // đã hết
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            assertThrows(NotFoundException.class,
                    () -> bidService.placeBid(1, "alice", 1_100L));
        }

        @Test
        @DisplayName("TC-BID-06: trạng thái auction không phải RUNNING ném NotFoundException")
        void nonRunningAuction_throwsNotFound() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            a.setStatus(AuctionStatus.OPEN);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            assertThrows(NotFoundException.class,
                    () -> bidService.placeBid(1, "alice", 1_100L));
        }

        @Test
        @DisplayName("TC-BID-07: seller tự đặt giá ném ValidationException")
        void sellerBidsOwnAuction_throwsValidation() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("seller", 9_000L, 0L);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "seller")).thenReturn(m);

            assertThrows(ValidationException.class,
                    () -> bidService.placeBid(1, "seller", 1_100L));
        }

        @Test
        @DisplayName("TC-BID-08: giá đặt thấp hơn currentPrice + step ném ValidationException")
        void bidTooLow_throwsValidation() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("alice", 5_000L, 0L);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(ValidationException.class,
                    () -> bidService.placeBid(1, "alice", 1_050L)); // < 1100
        }

        @Test
        @DisplayName("TC-BID-09: số dư không đủ ném FinanceException")
        void insufficientBalance_throwsFinance() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("alice", 500L, 0L); // balance < bid
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(FinanceException.class,
                    () -> bidService.placeBid(1, "alice", 1_100L));
        }

        @Test
        @DisplayName("TC-BID-10: Buy Now – khi giá >= buyNowPrice auction kết thúc ngay")
        void buyNow_closesAuctionImmediately() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            a.setBuyNowPrice(2_000L);
            Member m = member("alice", 10_000L, 0L);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);
            when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

            BidResult result = bidService.placeBid(1, "alice", 2_000L);

            assertEquals(2_000L, result.getCurrentPrice());
            // endTime phải được cập nhật (Buy Now đặt về now)
            verify(auctionDao).updateAuctionEndTime(eq(connection), eq(1), any(Timestamp.class));
        }

        @Test
        @DisplayName("TC-BID-11: winner hiện tại đặt lại – chỉ block phần chênh lệch")
        void currentWinnerRebids_blocksOnlyDelta() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            a.setWinnerAccountname("alice");
            // alice đang block 1000, balance = 5000
            Member alice = member("alice", 5_000L, 1_000L);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);
            when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

            BidResult result = bidService.placeBid(1, "alice", 1_100L);
            assertEquals("alice", result.getWinnerAccountname());
            // addBlockedBalance chỉ được gọi với delta = 100
            verify(userDao).addBlockedBalance(connection, "alice", 100L);
        }
    }

    // ── configureAutoBid ──────────────────────────────────────────────────

    @Nested
    @DisplayName("configureAutoBid()")
    class ConfigureAutoBid {

        @Test
        @DisplayName("TC-AUTO-01: cấu hình hợp lệ – upsert được gọi")
        void validConfig_upsertsCalled() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("alice", 5_000L, 0L);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);
            when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() ->
                    bidService.configureAutoBid(1, "alice", 3_000L, 100L));

            verify(autoBidDao).upsertAutoBid(connection, 1, "alice", 3_000L, 100L);
        }

        @Test
        @DisplayName("TC-AUTO-02: maxBid <= 0 ném ValidationException")
        void zeroMaxBid_throwsValidation() {
            assertThrows(ValidationException.class,
                    () -> bidService.configureAutoBid(1, "alice", 0L, 100L));
        }

        @Test
        @DisplayName("TC-AUTO-03: incrementAmount <= 0 ném ValidationException")
        void zeroIncrement_throwsValidation() {
            assertThrows(ValidationException.class,
                    () -> bidService.configureAutoBid(1, "alice", 3_000L, 0L));
        }

        @Test
        @DisplayName("TC-AUTO-04: maxBid thấp hơn giá tối thiểu tiếp theo ném ValidationException")
        void maxBidTooLow_throwsValidation() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L); // min next = 1100
            Member  m = member("alice", 5_000L, 0L);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(ValidationException.class,
                    () -> bidService.configureAutoBid(1, "alice", 1_000L, 100L));
        }

        @Test
        @DisplayName("TC-AUTO-05: seller tự cấu hình auto-bid ném ValidationException")
        void sellerConfiguresOwn_throwsValidation() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("seller", 9_000L, 0L);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "seller")).thenReturn(m);

            assertThrows(ValidationException.class,
                    () -> bidService.configureAutoBid(1, "seller", 3_000L, 100L));
        }

        @Test
        @DisplayName("TC-AUTO-06: số dư không đủ ném FinanceException")
        void insufficientBalance_throwsFinance() throws Exception {
            Auction a = runningAuction(1, "seller", 1_000L, 100L);
            Member  m = member("alice", 200L, 0L); // không đủ để bid 1100
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(m);

            assertThrows(FinanceException.class,
                    () -> bidService.configureAutoBid(1, "alice", 3_000L, 100L));
        }
    }

    // ── cancelAutoBid ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelAutoBid()")
    class CancelAutoBid {

        @Test
        @DisplayName("TC-CANCEL-01: hủy thành công")
        void cancel_success() throws Exception {
            when(autoBidDao.deactivateAutoBid(connection, 1, "alice")).thenReturn(true);
            assertDoesNotThrow(() -> bidService.cancelAutoBid(1, "alice"));
        }

        @Test
        @DisplayName("TC-CANCEL-02: auto-bid không tồn tại ném NotFoundException")
        void cancelNotFound_throwsNotFound() throws Exception {
            when(autoBidDao.deactivateAutoBid(connection, 1, "alice")).thenReturn(false);
            assertThrows(NotFoundException.class,
                    () -> bidService.cancelAutoBid(1, "alice"));
        }
    }

    // ── getBidHistoryPaged ────────────────────────────────────────────────

    @Nested
    @DisplayName("getBidHistoryPaged()")
    class GetBidHistory {

        @Test
        @DisplayName("TC-HIST-01: trả về PagedResponse đúng")
        void returnsPagedResponse() throws Exception {
            when(bidDao.getTotalBidsCount(connection, 1)).thenReturn(25L);
            when(bidDao.getBidsPaged(connection, 1, 10, 0)).thenReturn(Collections.emptyList());

            PagedResponse<Bid> resp = bidService.getBidHistoryPaged(1, 1, 10);

            assertEquals(25L, resp.getTotalItems());
            assertEquals(1,   resp.getCurrentPage());
            assertEquals(10,  resp.getPageSize());
        }
    }

    // ── utility ───────────────────────────────────────────────────────────

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
