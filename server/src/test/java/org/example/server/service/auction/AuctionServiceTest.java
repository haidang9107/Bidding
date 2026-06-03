package org.example.server.service.auction;

import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.product.Product;
import org.example.server.event.AuctionEndedEvent;
import org.example.server.event.AuctionStartedEvent;
import org.example.server.event.EventPublisher;
import org.example.server.exception.AuctionException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.*;
import org.example.server.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private TransactionManager txManager;
    @Mock private EventPublisher     eventPublisher;
    @Mock private ProductService     productService;
    @Mock private AuctionDao         auctionDao;
    @Mock private ProductDao         productDao;
    @Mock private UserDao            userDao;
    @Mock private TransactionDao     transactionDao;
    @Mock private AuctionMonitor     auctionMonitor;
    @Mock private Connection         connection;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() throws Exception {
        auctionService = new AuctionService(txManager, eventPublisher, productService);
        auctionService.setAuctionMonitor(auctionMonitor);

        setField(auctionService, "auctionDao",     auctionDao);
        setField(auctionService, "productDao",     productDao);
        setField(auctionService, "userDao",        userDao);
        setField(auctionService, "transactionDao", transactionDao);

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

    // ── helpers ───────────────────────────────────────────────────────────

    private Auction auction(int id, AuctionStatus status) {
        Auction a = new Auction();
        a.setAuctionId(id);
        a.setStatus(status);
        a.setSellerAccountname("seller");
        a.setCurrentPrice(1_000L);
        a.setStartingPrice(1_000L);
        a.setStepPrice(100L);
        a.setProductId(10);
        a.setEndTime(new Timestamp(System.currentTimeMillis() + 3_600_000));
        return a;
    }

    private static void setField(Object obj, String name, Object val) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }

    // ── startAuction ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("startAuction()")
    class StartAuction {

        @Test
        @DisplayName("TC-AUC-01: OPEN → RUNNING, publish AuctionStartedEvent")
        void openToRunning_publishesEvent() throws Exception {
            Auction a = auction(1, AuctionStatus.OPEN);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(auctionDao.updateStatus(connection, 1, AuctionStatus.RUNNING)).thenReturn(true);

            auctionService.startAuction(1);

            verify(auctionMonitor).scheduleAuctionEnd(eq(1), any(Timestamp.class));
            verify(eventPublisher).publish(any(AuctionStartedEvent.class));
        }

        @Test
        @DisplayName("TC-AUC-02: auction không OPEN – không làm gì cả")
        void nonOpen_doesNothing() throws Exception {
            Auction a = auction(1, AuctionStatus.RUNNING);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            auctionService.startAuction(1);

            verify(auctionDao, never()).updateStatus(any(), anyInt(), any());
        }

        @Test
        @DisplayName("TC-AUC-03: auction null – không crash")
        void nullAuction_doesNothing() throws Exception {
            when(auctionDao.getAuctionForUpdate(connection, 99)).thenReturn(null);

            assertDoesNotThrow(() -> auctionService.startAuction(99));
        }
    }

    // ── finishAuction ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("finishAuction()")
    class FinishAuction {

        @Test
        @DisplayName("TC-FIN-01: có winner – thanh toán, chuyển quyền sở hữu, publish event")
        void withWinner_settlesPayment() throws Exception {
            Auction a = auction(1, AuctionStatus.RUNNING);
            a.setWinnerAccountname("alice");

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(auctionDao.updateStatus(connection, 1, AuctionStatus.FINISHED)).thenReturn(true);

            auctionService.finishAuction(1);

            verify(userDao).addBalance(connection, "alice", -1_000L);
            verify(userDao).addBlockedBalance(connection, "alice", -1_000L);
            verify(userDao).addBalance(connection, "seller", 1_000L);
            verify(productService).transferOwnership(connection, 10, "alice");
            verify(transactionDao).insertTransaction(any(), eq("alice"), eq("seller"), any(), eq(10), eq(1_000L), eq(1), anyString());
            verify(eventPublisher).publish(any(AuctionEndedEvent.class));
        }

        @Test
        @DisplayName("TC-FIN-02: không có winner – giải phóng product flag")
        void noWinner_releasesProduct() throws Exception {
            Auction a = auction(1, AuctionStatus.RUNNING);
            a.setWinnerAccountname(null);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(auctionDao.updateStatus(connection, 1, AuctionStatus.FINISHED)).thenReturn(true);

            auctionService.finishAuction(1);

            verify(productDao).updateProductAuctionFlag(connection, 10, false);
            verify(userDao, never()).addBalance(any(), anyString(), anyLong());
        }

        @Test
        @DisplayName("TC-FIN-03: auction không RUNNING – không làm gì")
        void nonRunning_doesNothing() throws Exception {
            Auction a = auction(1, AuctionStatus.FINISHED);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            auctionService.finishAuction(1);

            verify(auctionDao, never()).updateStatus(any(), anyInt(), any());
        }
    }

    // ── cancelAuction ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelAuction()")
    class CancelAuction {

        @Test
        @DisplayName("TC-CAN-01: hủy thành công – trả về true, giải phóng blocked balance")
        void cancel_success() throws Exception {
            Auction a = auction(1, AuctionStatus.RUNNING);
            a.setWinnerAccountname("alice");

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(auctionDao.updateStatus(connection, 1, AuctionStatus.CANCELED)).thenReturn(true);

            boolean result = auctionService.cancelAuction(1);

            assertTrue(result);
            verify(productDao).updateProductAuctionFlag(connection, 10, false);
            verify(userDao).addBlockedBalance(connection, "alice", -1_000L);
            verify(eventPublisher).publish(any(AuctionEndedEvent.class));
        }

        @Test
        @DisplayName("TC-CAN-02: auction đã FINISHED – trả về false")
        void alreadyFinished_returnsFalse() throws Exception {
            Auction a = auction(1, AuctionStatus.FINISHED);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            assertFalse(auctionService.cancelAuction(1));
        }

        @Test
        @DisplayName("TC-CAN-03: auction đã CANCELED – trả về false")
        void alreadyCanceled_returnsFalse() throws Exception {
            Auction a = auction(1, AuctionStatus.CANCELED);
            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);

            assertFalse(auctionService.cancelAuction(1));
        }

        @Test
        @DisplayName("TC-CAN-04: không có winner – không gọi addBlockedBalance")
        void noWinner_noBlockedBalanceRelease() throws Exception {
            Auction a = auction(1, AuctionStatus.RUNNING);
            a.setWinnerAccountname(null);

            when(auctionDao.getAuctionForUpdate(connection, 1)).thenReturn(a);
            when(auctionDao.updateStatus(connection, 1, AuctionStatus.CANCELED)).thenReturn(true);

            auctionService.cancelAuction(1);

            verify(userDao, never()).addBlockedBalance(any(), anyString(), anyLong());
        }
    }

    // ── openAuctionForProduct ─────────────────────────────────────────────

    @Nested
    @DisplayName("openAuctionForProduct()")
    class OpenAuctionForProduct {

        @Test
        @DisplayName("TC-OPEN-01: startingPrice <= 0 ném ValidationException")
        void negativeStartPrice_throws() {
            assertThrows(ValidationException.class, () ->
                    auctionService.openAuctionForProduct(1, "seller", 0L, null, null, null, null));
        }

        @Test
        @DisplayName("TC-OPEN-02: endTime trước startTime ném IllegalArgumentException")
        void endBeforeStart_throws() throws Exception {
            Product p = mock(Product.class);
            when(p.getOwnerAccountname()).thenReturn("seller");
            when(p.isInAuction()).thenReturn(false);
            when(productDao.getProductForUpdate(connection, 1)).thenReturn(p);

            Timestamp start = new Timestamp(System.currentTimeMillis() + 3_600_000);
            Timestamp end   = new Timestamp(System.currentTimeMillis());           // trước start

            // SỬA Ở ĐÂY: Bắt đúng IllegalArgumentException do Auction.Builder ném ra
            assertThrows(IllegalArgumentException.class, () ->
                    auctionService.openAuctionForProduct(1, "seller", 500L, null, null, start, end));
        }

        @Test
        @DisplayName("TC-OPEN-03: product đang trong auction ném AuctionException")
        void productAlreadyInAuction_throws() throws Exception {
            Product p = mock(Product.class);
            when(p.getOwnerAccountname()).thenReturn("seller");
            when(p.isInAuction()).thenReturn(true);
            when(productDao.getProductForUpdate(connection, 1)).thenReturn(p);

            assertThrows(AuctionException.class, () ->
                    auctionService.openAuctionForProduct(1, "seller", 500L, null, null, null, null));
        }

        @Test
        @DisplayName("TC-OPEN-04: seller khác owner ném ValidationException")
        void wrongOwner_throws() throws Exception {
            Product p = mock(Product.class);
            when(p.getOwnerAccountname()).thenReturn("realOwner");
            when(productDao.getProductForUpdate(connection, 1)).thenReturn(p);

            assertThrows(ValidationException.class, () ->
                    auctionService.openAuctionForProduct(1, "imposter", 500L, null, null, null, null));
        }
    }
}
