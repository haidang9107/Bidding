package controller;

import org.example.dto.request.AutoBidRequest;
import org.example.dto.request.BidHistoryRequest;
import org.example.dto.request.BidRequest;
import org.example.dto.response.BidResult;
import org.example.dto.response.PagedResponse;
import org.example.model.Bid;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.controller.BidController;
import org.example.server.service.bid.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BidControllerTest {

    private BidService bidServiceMock;
    private BidController bidController;

    @BeforeEach
    void setUp() {
        // Mock tầng BidService
        bidServiceMock = Mockito.mock(BidService.class);
        bidController = new BidController(bidServiceMock);
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: handleGetBidHistory
    // ====================================================================

    @Test
    void testHandleGetBidHistory_Success() {
        BidHistoryRequest historyReq = new BidHistoryRequest();
        historyReq.setAuctionId(1);
        historyReq.setPage(1);
        historyReq.setPageSize(5);

        // Giả lập dữ liệu danh sách lịch sử đấu giá
        Bid mockBid1 = Mockito.mock(Bid.class);
        Bid mockBid2 = Mockito.mock(Bid.class);
        PagedResponse<Bid> mockPagedResponse = new PagedResponse<>(
                Arrays.asList(mockBid1, mockBid2), 2, 1, 5
        );

        Mockito.when(bidServiceMock.getBidHistoryPaged(1, 1, 5)).thenReturn(mockPagedResponse);

        Response<PagedResponse<Bid>> response = bidController.handleGetBidHistory(historyReq);

        assertNotNull(response);
        assertEquals(MessageType.BID_HISTORY, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Bid history fetched successfully", response.getMessage());
        assertEquals(2, response.getData().getItems().size());
    }

    @Test
    void testHandleGetBidHistory_NullRequest() {
        Response<PagedResponse<Bid>> response = bidController.handleGetBidHistory(null);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("History request data required", response.getMessage());
        assertNull(response.getData());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handlePlaceBid
    // ====================================================================

    @Test
    void testHandlePlaceBid_Success() {
        BidRequest bidReq = new BidRequest();
        bidReq.setAuctionId(10);
        bidReq.setAmount(500000L);
        String username = "bidder_pro";

        BidResult mockResult = Mockito.mock(BidResult.class);
        Mockito.when(bidServiceMock.placeBid(10, username, 500000L)).thenReturn(mockResult);

        Response<BidResult> response = bidController.handlePlaceBid(bidReq, username);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Bid placed successfully", response.getMessage());
        assertEquals(mockResult, response.getData());
    }

    @Test
    void testHandlePlaceBid_NullRequest() {
        Response<BidResult> response = bidController.handlePlaceBid(null, "bidder_pro");

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Bid data required", response.getMessage());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: handleConfigureAutoBid
    // ====================================================================

    @Test
    void testHandleConfigureAutoBid_Success() {
        AutoBidRequest autoBidReq = new AutoBidRequest();
        autoBidReq.setAuctionId(20);
        autoBidReq.setMaxBid(2000000L);
        autoBidReq.setIncrementAmount(50000L);
        String username = "auto_bidder";

        Mockito.doNothing().when(bidServiceMock).configureAutoBid(20, username, 2000000L, 50000L);

        Response<String> response = bidController.handleConfigureAutoBid(autoBidReq, username);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Auto bid configured successfully", response.getMessage());

        // Kiểm tra xem service thực sự đã được gọi chuẩn xác hay chưa
        Mockito.verify(bidServiceMock, Mockito.times(1))
                .configureAutoBid(20, username, 2000000L, 50000L);
    }

    @Test
    void testHandleConfigureAutoBid_NullRequest() {
        Response<String> response = bidController.handleConfigureAutoBid(null, "auto_bidder");

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Auto bid data required", response.getMessage());
    }

    // ====================================================================
    // 4. TEST CASE CHO HÀM: handleCancelAutoBid
    // ====================================================================

    @Test
    void testHandleCancelAutoBid_Success() {
        AutoBidRequest autoBidReq = new AutoBidRequest();
        autoBidReq.setAuctionId(20);
        String username = "auto_bidder";

        Mockito.doNothing().when(bidServiceMock).cancelAutoBid(20, username);

        Response<String> response = bidController.handleCancelAutoBid(autoBidReq, username);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Auto bid canceled successfully", response.getMessage());

        Mockito.verify(bidServiceMock, Mockito.times(1)).cancelAutoBid(20, username);
    }

    @Test
    void testHandleCancelAutoBid_NullRequest() {
        Response<String> response = bidController.handleCancelAutoBid(null, "auto_bidder");

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Auto bid data required", response.getMessage());
    }
}