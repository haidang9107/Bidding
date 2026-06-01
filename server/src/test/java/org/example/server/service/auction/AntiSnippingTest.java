package org.example.server.service.auction;

import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.server.repository.AuctionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AntiSnipping}.
 *
 * NOTE: AntiSnipping reads ANTI_SNIP_WINDOW_MS and ANTI_SNIP_EXTENSION_MS
 * from Config/env.  Tests assume defaults (window=3min=180000ms,
 * extension=5min=300000ms) or are written to be independent of exact values
 * by only asserting that the end time was extended.
 */
@ExtendWith(MockitoExtension.class)
class AntiSnippingTest {

    @Mock private AuctionDao auctionDao;
    @Mock private Connection  connection;

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setAuctionId(1);
        auction.setStatus(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("TC-SNIP-01: bid trong cửa sổ cuối – endTime được gia hạn")
    void bidInWindow_extendsEndTime() throws SQLException {
        // endTime = 1 phút từ bây giờ (trong window 3 phút)
        Timestamp originalEnd = new Timestamp(System.currentTimeMillis() + 60_000L);
        auction.setEndTime(originalEnd);

        when(auctionDao.updateAuctionEndTime(eq(connection), eq(1), any(Timestamp.class)))
                .thenReturn(true);

        AntiSnipping.process(connection, auction, auctionDao);

        // endTime phải được cập nhật
        assertTrue(auction.getEndTime().after(originalEnd),
                "endTime phải được gia hạn sau anti-snipping");

        ArgumentCaptor<Timestamp> cap = ArgumentCaptor.forClass(Timestamp.class);
        verify(auctionDao).updateAuctionEndTime(eq(connection), eq(1), cap.capture());
        assertTrue(cap.getValue().after(originalEnd));
    }

    @Test
    @DisplayName("TC-SNIP-02: bid ngoài cửa sổ cuối – endTime không thay đổi")
    void bidOutsideWindow_noExtension() throws SQLException {
        // endTime = 10 phút (ngoài window 3 phút)
        Timestamp originalEnd = new Timestamp(System.currentTimeMillis() + 600_000L);
        auction.setEndTime(originalEnd);

        AntiSnipping.process(connection, auction, auctionDao);

        assertEquals(originalEnd, auction.getEndTime(), "endTime không được thay đổi");
        verify(auctionDao, never()).updateAuctionEndTime(any(), anyInt(), any());
    }

    @Test
    @DisplayName("TC-SNIP-03: endTime = null – không crash")
    void nullEndTime_doesNothing() throws SQLException {
        auction.setEndTime(null);

        assertDoesNotThrow(() -> AntiSnipping.process(connection, auction, auctionDao));
        verify(auctionDao, never()).updateAuctionEndTime(any(), anyInt(), any());
    }

    @Test
    @DisplayName("TC-SNIP-04: endTime đã qua – không gia hạn (diff <= 0)")
    void pastEndTime_doesNothing() throws SQLException {
        Timestamp past = new Timestamp(System.currentTimeMillis() - 1_000L);
        auction.setEndTime(past);

        AntiSnipping.process(connection, auction, auctionDao);

        verify(auctionDao, never()).updateAuctionEndTime(any(), anyInt(), any());
    }
}
