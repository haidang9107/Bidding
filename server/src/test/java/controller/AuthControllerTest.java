package controller;

import org.example.dto.request.LoginRequest;
import org.example.dto.request.SignupRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.server.service.user.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    private AuthService authServiceMock;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Khởi tạo Mock Service và Controller
        authServiceMock = Mockito.mock(AuthService.class);
        authController = new AuthController(authServiceMock);
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: authenticateAndGetUser
    // ====================================================================

    @Test
    void testAuthenticateAndGetUser_Success() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setPassword("password123");

        // Mock đối tượng User (Bypass lỗi abstract class)
        User mockUser = Mockito.mock(User.class);
        Mockito.when(mockUser.getAccountname()).thenReturn("testuser");

        // Cấu hình mock service trả về mock user khi truyền đúng thông tin
        Mockito.when(authServiceMock.authenticate("testuser", "password123")).thenReturn(mockUser);

        User result = authController.authenticateAndGetUser(loginReq);

        assertNotNull(result);
        assertEquals("testuser", result.getAccountname());
    }

    @Test
    void testAuthenticateAndGetUser_NullRequest() {
        User result = authController.authenticateAndGetUser(null);
        assertNull(result);
    }

    @Test
    void testAuthenticateAndGetUser_NullUsername() {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(null); // Username bị null
        loginReq.setPassword("password123");

        User result = authController.authenticateAndGetUser(loginReq);
        assertNull(result);
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: handleSignup
    // ====================================================================

    @Test
    void testHandleSignup_Success() {
        SignupRequest signupReq = new SignupRequest();
        signupReq.setUsername("newuser");
        signupReq.setPassword("securepass");
        signupReq.setEmail("newuser@example.com");

        // Hàm void của service cấu hình doNothing
        Mockito.doNothing().when(authServiceMock).register("newuser", "securepass", "newuser@example.com");

        Response<?> response = authController.handleSignup(signupReq);

        assertNotNull(response);
        assertEquals(MessageType.SUCCESS, response.getType());
        assertTrue(response.isSuccess());
        assertEquals("Registration successful", response.getMessage());

        // Kiểm tra xem tầng service đã thực sự chạy chưa
        Mockito.verify(authServiceMock, Mockito.times(1))
                .register("newuser", "securepass", "newuser@example.com");
    }

    @Test
    void testHandleSignup_NullRequest() {
        Response<?> response = authController.handleSignup(null);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Invalid signup data", response.getMessage());
    }

    @Test
    void testHandleSignup_NullUsername() {
        SignupRequest signupReq = new SignupRequest();
        signupReq.setUsername(null); // Thiếu thông tin bắt buộc
        signupReq.setPassword("securepass");
        signupReq.setEmail("newuser@example.com");

        Response<?> response = authController.handleSignup(signupReq);

        assertNotNull(response);
        assertEquals(MessageType.ERROR, response.getType());
        assertFalse(response.isSuccess());
        assertEquals("Invalid signup data", response.getMessage());
    }
}