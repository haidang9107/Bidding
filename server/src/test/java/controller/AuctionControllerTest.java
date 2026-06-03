package controller;

import org.example.dto.request.PaginationRequest;
import org.example.dto.request.ProductAddRequest;
import org.example.dto.response.PagedResponse;
import org.example.dto.response.ProductResponse;
import org.example.model.Auction;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.controller.AuctionController;
import org.example.server.exception.NotFoundException;
import org.example.server.service.auction.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AuctionControllerTest {

    private AuctionService auctionServiceMock;
    private AuctionController auctionController;

    @BeforeEach
    void setUp() {
        // Mock tầng AuctionService
        auctionServiceMock = Mockito.mock(AuctionService.class);
        auctionController = new AuctionController(auctionServiceMock);
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: handleGetAllAuctions
    // ====================================================================

    @Test
    void testHandleGetAllAuctions_Success() {
        // Giả lập dữ liệu cuộc đấu giá (Mock Auction)
        Auction mockAuction1 = Mockito.mock(Auction.class);
        Auction mockAuction2 = Mockito.mock(Auction.class);

        PagedResponse<Auction> mockPagedResponse = new PagedResponse<>(
                Arrays.asList(mockAuction1, mockAuction2), 2, 1, 10
        );

        // Định nghĩa hành vi cho Mock Service
        Mockito.when(auctionServiceMock.getAuctionsPaged(1, 10)).thenReturn(mockPagedResponse);

        PaginationRequest pagReq = new PaginationRequest(1, 10);
        Response<?> response = auctionController.handleGetAllAuctions(pagReq);

        // Kiểm tra kết quả trả về
        assertNotNull(response);
        assertEquals(MessageType.PRODUCT_LIST, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Auctions fetched successfully", response.getMessage());

        PagedResponse<?> finalData = (PagedResponse<?>) response.getData();
        assertEquals(2, finalData.getItems().size());
        assertEquals(2, finalData.getTotalItems());
    }

    @Test
    void testHandleGetAllAuctions_NullRequest_FallbackToDefault() {
        // 1. Đổi số 0 thành 0L (để khớp với kiểu long của biến totalItems)
        PagedResponse<Auction> emptyPagedResponse = new PagedResponse<>(Collections.emptyList(), 0L, 1, 10);

        // 2. Dùng Mockito.anyInt() để bao trọn mọi tham số mặc định mà Controller có thể gọi, tránh bị trả về null
        Mockito.when(auctionServiceMock.getAuctionsPaged(Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(emptyPagedResponse);

        // Truyền null request để kiểm tra logic fallback
        Response<?> response = auctionController.handleGetAllAuctions(null);

        assertNotNull(response);
        assertEquals(MessageType.PRODUCT_LIST, response.getType());

        // Xác nhận rằng Controller thực sự có gọi hàm getAuctionsPaged
        Mockito.verify(auctionServiceMock, Mockito.times(1))
                .getAuctionsPaged(Mockito.anyInt(), Mockito.anyInt());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handleGetAuctionDetail
    // ====================================================================

    @Test
    void testHandleGetAuctionDetail_Success() {
        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(auctionServiceMock.getAuctionById(101)).thenReturn(mockAuction);

        Response<ProductResponse> response = auctionController.handleGetAuctionDetail(101);

        assertNotNull(response);
        assertEquals(MessageType.PRODUCT_DETAIL, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Auction found", response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    void testHandleGetAuctionDetail_NotFound_ThrowsException() {
        // Giả lập ID không tồn tại hệ thống trả về null
        Mockito.when(auctionServiceMock.getAuctionById(404)).thenReturn(null);

        // Kiểm tra xem hàm có ném ra đúng NotFoundException hay không
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            auctionController.handleGetAuctionDetail(404);
        });

        assertEquals("Auction not found with ID: 404", exception.getMessage());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: handleCreateAuction
    // ====================================================================

    @Test
    void testHandleCreateAuction_Success() {
        ProductAddRequest request = new ProductAddRequest();
        String sellerAccount = "test_seller";

        // Hàm createAuction của service trả về void nên chỉ cần cấu hình nhận tham số ngẫu nhiên
        Mockito.doNothing().when(auctionServiceMock).createAuction(Mockito.any(ProductAddRequest.class), Mockito.anyString());

        Response<String> response = auctionController.handleCreateAuction(request, sellerAccount);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Auction created successfully", response.getMessage());
        assertNull(response.getData());

        // Kiểm tra xem service thực sự đã được gọi chuẩn xác 1 lần hay chưa
        Mockito.verify(auctionServiceMock, Mockito.times(1)).createAuction(request, sellerAccount);
    }
}