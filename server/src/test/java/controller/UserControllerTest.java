package controller;

import org.example.dto.request.UserProfileUpdateRequest;
import org.example.dto.response.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.controller.UserController;
import org.example.server.network.SessionManager;
import org.example.server.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserService userServiceMock;
    private UserController userController;
    private SocketChannel socketChannelMock;
    private User userMock;

    // Đối tượng quản lý việc mock static của lớp SessionManager
    private MockedStatic<SessionManager> sessionManagerMockedStatic;

    @BeforeEach
    void setUp() {
        userServiceMock = Mockito.mock(UserService.class);
        userController = new UserController(userServiceMock);
        socketChannelMock = Mockito.mock(SocketChannel.class);
        userMock = Mockito.mock(User.class);

        // Kích hoạt Mock Static cho lớp SessionManager trước mỗi test case
        sessionManagerMockedStatic = Mockito.mockStatic(SessionManager.class);
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc phải đóng mock static sau mỗi test case để tránh xung đột luồng
        sessionManagerMockedStatic.close();
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: handleGetProfile
    // ====================================================================

    @Test
    void testHandleGetProfile_Success() {
        // Giả lập SessionManager trả về user hợp lệ khi truyền vào channel
        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(userMock);
        Mockito.when(userMock.getAccountname()).thenReturn("test_user");

        Response<UserResponse> response = userController.handleGetProfile(socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Profile fetched successfully", response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    void testHandleGetProfile_Unauthorized() {
        // Giả lập người dùng chưa đăng nhập (SessionManager trả về null)
        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(null);

        Response<UserResponse> response = userController.handleGetProfile(socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Unauthorized", response.getMessage());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handleUpdateProfile
    // ====================================================================

    @Test
    void testHandleUpdateProfile_Success_BothEmailAndAvatar() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setEmail("new_email@example.com");
        request.setAvt("/path/to/new_avatar.png");

        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(userMock);
        Mockito.when(userMock.getAccountname()).thenReturn("test_user");

        // Cấu hình service cập nhật thông tin thành công
        Mockito.when(userServiceMock.updateEmail("test_user", "new_email@example.com")).thenReturn(true);
        Mockito.when(userServiceMock.updateAvatar("test_user", "/path/to/new_avatar.png")).thenReturn(true);

        Response<String> response = userController.handleUpdateProfile(request, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Profile updated successfully", response.getMessage());

        // Kiểm tra xem in-memory session của User thực sự đã được đồng bộ gọi hàm gán dữ liệu mới chưa
        Mockito.verify(userMock, Mockito.times(1)).setEmail("new_email@example.com");
        Mockito.verify(userMock, Mockito.times(1)).setAvt("/path/to/new_avatar.png");
    }

    @Test
    void testHandleUpdateProfile_NullRequest() {
        Response<String> response = userController.handleUpdateProfile(null, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals("Update data required", response.getMessage());
    }

    @Test
    void testHandleUpdateProfile_Unauthorized() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(null);

        Response<String> response = userController.handleUpdateProfile(request, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals("Unauthorized", response.getMessage());
    }

    @Test
    void testHandleUpdateProfile_Exception_ReturnsInternalServerError() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setEmail("error@test.com");

        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(userMock);
        Mockito.when(userMock.getAccountname()).thenReturn("test_user");

        // Giả lập lỗi runtime ở tầng database/service
        Mockito.when(userServiceMock.updateEmail(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new RuntimeException("DB error"));

        Response<String> response = userController.handleUpdateProfile(request, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals("Internal server error", response.getMessage());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: handleUpdateAvatar
    // ====================================================================

    @Test
    void testHandleUpdateAvatar_Success() {
        String newAvatarPath = "/images/avt.jpg";

        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(userMock);
        Mockito.when(userMock.getAccountname()).thenReturn("test_user");
        Mockito.when(userServiceMock.updateAvatar("test_user", newAvatarPath)).thenReturn(true);

        Response<String> response = userController.handleUpdateAvatar(newAvatarPath, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Avatar updated successfully", response.getMessage());
        Mockito.verify(userMock, Mockito.times(1)).setAvt(newAvatarPath);
    }

    @Test
    void testHandleUpdateAvatar_NullOrBlankPath() {
        Response<String> responseNull = userController.handleUpdateAvatar(null, socketChannelMock);
        assertEquals("Avatar path required", responseNull.getMessage());

        Response<String> responseBlank = userController.handleUpdateAvatar("   ", socketChannelMock);
        assertEquals("Avatar path required", responseBlank.getMessage());
    }

    @Test
    void testHandleUpdateAvatar_FailedToUpdate() {
        String newAvatarPath = "/images/avt.jpg";

        sessionManagerMockedStatic.when(() -> SessionManager.getUser(socketChannelMock)).thenReturn(userMock);
        Mockito.when(userMock.getAccountname()).thenReturn("test_user");
        // Giả lập tầng service trả về kết quả false (ví dụ do lỗi logic)
        Mockito.when(userServiceMock.updateAvatar("test_user", newAvatarPath)).thenReturn(false);

        Response<String> response = userController.handleUpdateAvatar(newAvatarPath, socketChannelMock);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertEquals("Failed to update avatar", response.getMessage());
    }
}