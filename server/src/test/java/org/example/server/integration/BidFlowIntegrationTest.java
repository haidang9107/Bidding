package org.example.server.integration;

import org.example.dto.response.BidResult;
import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.user.Member;
import org.example.server.event.EventPublisher;
import org.example.server.event.NewBidPlacedEvent;
import org.example.server.exception.FinanceException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.*;
import org.example.server.service.auction.AuctionMonitor;
import org.example.server.service.bid.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration-style tests that exercise multiple collaborating objects
 * (BidService → AutoBid cascade, Buy-Now flow, etc.) without a real DB.
 *
 * These tests are "integration" in the sense that they validate the
 * end-to-end business flow across service methods rather than a single
 * isolated unit, while still using mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class BidFlowIntegrationTest {

    @Mock private TransactionManager txManager;
    @Mock private EventPublisher     eventPublisher;
    @Mock private AuctionMonitor     auctionMonitor;
    @Mock private AuctionDao         auctionDao;
    @Mock private BidDao             bidDao;
    @Mock private UserDao            userDao;
    @Mock private AutoBidDao         autoBidDao;
    @Mock private Connection         connection;

    private BidService bidService;

    @BeforeEach
    void setUp() throws Exception {
        bidService = new BidService(txManager, eventPublisher, auctionMonitor);
        setField(bidService, "auctionDao", auctionDao);
        setField(bidService, "bidDao",     bidDao);
        setField(bidService, "userDao",    userDao);
        setField(bidService, "autoBidDao", autoBidDao);

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(txManager).execute(any());

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalRunnable fn = inv.getArgument(0);
            fn.execute(connection);
            return null;
        }).when(txManager).run(any());
    }

    // ── Kịch bản 1: Đặt giá thủ công ─────────────────────────────────────

    @Test
    @DisplayName("IT-01: User thay thế người dẫn đầu – blocked balance chuyển đúng")
    void newLeader_releasesOldLeaderBalance() throws Exception {
        Auction a = makeAuction(1, "seller", 1_000L, 100L, "bob"); // bob đang dẫn
        Member alice = makeMember("alice", 5_000L, 0L);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);
        when(userDao.findByAccountnameForUpdate(connection, "bob"))
                .thenReturn(makeMember("bob", 3_000L, 1_000L));
        when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

        bidService.placeBid(1, "alice", 1_100L);

        // bob bị giải phóng blocked = -1000
        verify(userDao).addBlockedBalance(connection, "bob", -1_000L);
        // alice bị block toàn bộ 1100
        verify(userDao).addBlockedBalance(connection, "alice", 1_100L);
    }

    // ── Kịch bản 2: Auto-bid cascade ─────────────────────────────────────

    @Test
    @DisplayName("IT-02: Auto-bid kích hoạt sau khi đặt giá thủ công")
    void manualBid_triggersAutoBidCascade() throws Exception {
        // alice đặt thủ công 1100, sau đó auto-bid của carol (max=2000) được kích hoạt
        Auction a = makeAuction(1, "seller", 1_000L, 100L, null);
        Member  alice = makeMember("alice", 5_000L, 0L);
        Member  carol = makeMember("carol", 5_000L, 0L);

        org.example.model.AutoBid carolAutoBid = new org.example.model.AutoBid(0, 1, "carol", 2_000L, 100L, true, null, null);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        // First applyBid: lock alice (no old winner)
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);
        // After alice bids, auto-bids run: carol tries to outbid
        when(autoBidDao.findAllActiveForAuction(connection, 1))
                .thenReturn(List.of(carolAutoBid));
        when(userDao.findByAccountnameForUpdate(connection, "carol")).thenReturn(carol);

        BidResult result = bidService.placeBid(1, "alice", 1_100L);

        // carol should have outbid alice
        assertEquals("carol", result.getWinnerAccountname());
        assertTrue(result.isAutoBidApplied());
    }

    // ── Kịch bản 3: Buy-Now flow ──────────────────────────────────────────

    @Test
    @DisplayName("IT-03: Buy Now – giá đặt bằng buyNowPrice kết thúc phiên ngay")
    void buyNow_flow() throws Exception {
        Auction a = makeAuction(1, "seller", 1_000L, 100L, null);
        a.setBuyNowPrice(5_000L);
        Member alice = makeMember("alice", 10_000L, 0L);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);
        when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

        BidResult result = bidService.placeBid(1, "alice", 5_000L);

        assertEquals(5_000L, result.getCurrentPrice());
        // endTime phải được cập nhật → auction đóng ngay
        verify(auctionDao).updateAuctionEndTime(eq(connection), eq(1), any(Timestamp.class));
        // Sau Buy Now, auto-bid loop ngay lập tức thoát do endTime <= now
        verify(bidDao).insertBid(eq(connection), eq(1), eq("alice"), eq(5_000L), eq(false));
    }

    // ── Kịch bản 4: Concurrent bid ───────────────────────────────────────

    @Test
    @DisplayName("IT-04: Giá đặt vừa đúng minimum (currentPrice + step) – hợp lệ")
    void bidAtExactMinimum_accepted() throws Exception {
        Auction a = makeAuction(1, "seller", 1_000L, 100L, null);
        Member alice = makeMember("alice", 5_000L, 0L);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);
        when(autoBidDao.findAllActiveForAuction(connection, 1)).thenReturn(Collections.emptyList());

        BidResult result = bidService.placeBid(1, "alice", 1_100L); // chính xác min
        assertEquals(1_100L, result.getCurrentPrice());
    }

    @Test
    @DisplayName("IT-05: Giá đặt kém 1 đơn vị so với minimum – bị từ chối")
    void bidOneLessThanMinimum_rejected() throws Exception {
        Auction a = makeAuction(1, "seller", 1_000L, 100L, null);
        Member alice = makeMember("alice", 5_000L, 0L);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);

        assertThrows(ValidationException.class,
                () -> bidService.placeBid(1, "alice", 1_099L));
    }

    @Test
    @DisplayName("IT-06: Toàn bộ balance bị block – không thể đặt giá mới")
    void fullyBlockedBalance_cannotBid() throws Exception {
        // balance = 2000, blocked = 2000 → available = 0
        Auction a = makeAuction(1, "seller", 1_000L, 100L, null);
        Member alice = makeMember("alice", 2_000L, 2_000L);

        when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
        when(userDao.findByAccountnameForUpdate(connection, "alice")).thenReturn(alice);

        assertThrows(FinanceException.class,
                () -> bidService.placeBid(1, "alice", 1_100L));
    }

    // ── utilities ─────────────────────────────────────────────────────────

    private Auction makeAuction(int id, String seller, long price, long step, String winner) {
        Auction a = new Auction();
        a.setAuctionId(id);
        a.setSellerAccountname(seller);
        a.setCurrentPrice(price);
        a.setStepPrice(step);
        a.setStartingPrice(price);
        a.setStatus(AuctionStatus.RUNNING);
        a.setEndTime(new Timestamp(System.currentTimeMillis() + 3_600_000));
        a.setWinnerAccountname(winner);
        return a;
    }

    private Member makeMember(String acct, long balance, long blocked) {
        return new Member(acct, "hashed", acct + "@test.com", null, 0, balance, blocked);
    }

    private static void setField(Object obj, String name, Object val) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
