package controller;

import org.example.dto.request.AdminUserControlRequest;
import org.example.dto.request.AuctionCancelRequest;
import org.example.dto.request.PaginationRequest;
import org.example.dto.response.PagedResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.server.service.user.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    private AdminService adminServiceMock;
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        // Gọi tường minh qua lớp Mockito để ép IDE nhận diện Symbol
        adminServiceMock = Mockito.mock(AdminService.class);
        adminController = new AdminController(adminServiceMock);
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: handleGetAllUsers
    // ====================================================================

    @Test
    void testHandleGetAllUsers_Success() {
        User mockUser1 = Mockito.mock(User.class);
        Mockito.when(mockUser1.getAccountname()).thenReturn("user1");
        Mockito.when(mockUser1.getFullname()).thenReturn("User One");
        Mockito.when(mockUser1.getEmail()).thenReturn("user1@test.com");

        User mockUser2 = Mockito.mock(User.class);
        Mockito.when(mockUser2.getAccountname()).thenReturn("user2");
        Mockito.when(mockUser2.getFullname()).thenReturn("User Two");
        Mockito.when(mockUser2.getEmail()).thenReturn("user2@test.com");

        PagedResponse<User> mockPagedResponse = new PagedResponse<>(
                Arrays.asList(mockUser1, mockUser2), 2, 1, 10
        );

        Mockito.when(adminServiceMock.getUsersPaged(1, 10)).thenReturn(mockPagedResponse);

        PaginationRequest pagReq = new PaginationRequest(1, 10);
        Response<?> response = adminController.handleGetAllUsers(pagReq);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());

        PagedResponse<?> finalData = (PagedResponse<?>) response.getData();
        assertEquals(2, finalData.getItems().size());
    }

    @Test
    void testHandleGetAllUsers_NullRequest_FallbackToDefault() {
        PagedResponse<User> emptyPagedResponse = new PagedResponse<>(Collections.emptyList(), 0, 1, 10);
        Mockito.when(adminServiceMock.getUsersPaged(1, 10)).thenReturn(emptyPagedResponse);

        Response<?> response = adminController.handleGetAllUsers(null);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        Mockito.verify(adminServiceMock, Mockito.times(1)).getUsersPaged(1, 10);
    }

    @Test
    void testHandleGetAllUsers_Exception_ReturnsErrorResponse() {
        Mockito.when(adminServiceMock.getUsersPaged(Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(new RuntimeException("Database down"));

        PaginationRequest pagReq = new PaginationRequest(1, 10);
        Response<?> response = adminController.handleGetAllUsers(pagReq);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handleBanUser
    // ====================================================================

    @Test
    void testHandleBanUser_Success() {
        AdminUserControlRequest request = new AdminUserControlRequest();
        request.setTargetAccountname("bad_user");
        request.setStatus(2);

        Mockito.when(adminServiceMock.updateUserStatus("bad_user", 2)).thenReturn(true);

        Response<String> response = adminController.handleBanUser(request);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
    }

    @Test
    void testHandleBanUser_UserNotFound() {
        AdminUserControlRequest request = new AdminUserControlRequest();
        request.setTargetAccountname("non_existent_user");
        request.setStatus(2);

        Mockito.when(adminServiceMock.updateUserStatus("non_existent_user", 2)).thenReturn(false);

        Response<String> response = adminController.handleBanUser(request);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }

    @Test
    void testHandleBanUser_NullRequest() {
        Response<String> response = adminController.handleBanUser(null);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }

    @Test
    void testHandleBanUser_Exception() {
        AdminUserControlRequest request = new AdminUserControlRequest();
        request.setTargetAccountname("error_user");
        request.setStatus(1);

        Mockito.when(adminServiceMock.updateUserStatus(Mockito.anyString(), Mockito.anyInt()))
                .thenThrow(new RuntimeException("Error updating"));

        Response<String> response = adminController.handleBanUser(request);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: handleCancelAuction
    // ====================================================================

    @Test
    void testHandleCancelAuction_Success() {
        AuctionCancelRequest request = new AuctionCancelRequest();
        request.setAuctionId(101);

        Mockito.when(adminServiceMock.cancelAuction(101)).thenReturn(true);

        Response<String> response = adminController.handleCancelAuction(request);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
    }

    @Test
    void testHandleCancelAuction_NotFound() {
        AuctionCancelRequest request = new AuctionCancelRequest();
        request.setAuctionId(404);

        Mockito.when(adminServiceMock.cancelAuction(404)).thenReturn(false);

        Response<String> response = adminController.handleCancelAuction(request);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }

    @Test
    void testHandleCancelAuction_NullRequest() {
        Response<String> response = adminController.handleCancelAuction(null);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }

    @Test
    void testHandleCancelAuction_Exception() {
        AuctionCancelRequest request = new AuctionCancelRequest();
        request.setAuctionId(999);

        Mockito.when(adminServiceMock.cancelAuction(Mockito.anyInt()))
                .thenThrow(new RuntimeException("Error canceling"));

        Response<String> response = adminController.handleCancelAuction(request);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
    }
}