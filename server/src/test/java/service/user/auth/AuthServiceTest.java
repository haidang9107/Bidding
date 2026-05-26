package service.user.auth;

import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.model.user.User;
import org.example.server.exception.AuthException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.TransactionManager;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.user.auth.PasswordHashing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private TransactionManager txManagerMock;
    private AuthService authService;

    // Đối tượng quản lý việc mock static của lớp PasswordHashing
    private MockedStatic<PasswordHashing> passwordHashingMockedStatic;

    @BeforeEach
    void setUp() {
        txManagerMock = Mockito.mock(TransactionManager.class);
        authService = new AuthService(txManagerMock);

        // Kích hoạt Mock Static cho PasswordHashing trước mỗi kịch bản test
        passwordHashingMockedStatic = Mockito.mockStatic(PasswordHashing.class);
    }

    @AfterEach
    void tearDown() {
        // Luôn luôn đóng mock static sau mỗi test case
        passwordHashingMockedStatic.close();
    }

    // ====================================================================
    // 1. TEST CASE CHO HÀM: authenticate (Đăng nhập)
    // ====================================================================

    @SuppressWarnings("unchecked") // Bỏ qua cảnh báo ép kiểu an toàn của Java
    @Test
    void testAuthenticate_Success() throws Exception {
        User mockUser = Mockito.mock(User.class);
        Mockito.when(mockUser.getPassword()).thenReturn("hashed_password");
        Mockito.when(mockUser.getStatus()).thenReturn(0); // 0 = Hoạt động bình thường

        // 1. MOCK USERDAO: Tạo UserDao giả và tiêm thẳng vào biến private của authService
        org.example.server.repository.UserDao userDaoMock = Mockito.mock(org.example.server.repository.UserDao.class);
        java.lang.reflect.Field userDaoField = org.example.server.service.user.auth.AuthService.class.getDeclaredField("userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(authService, userDaoMock);

        // Định nghĩa: Khi tìm tài khoản "testuser" thì trả về mockUser luôn, không chạy vào DB thật nữa
        Mockito.when(userDaoMock.findByAccountname(Mockito.any(), Mockito.eq("testuser"))).thenReturn(mockUser);

        // 2. MOCK TXMANAGER: Bẻ khóa Lambda ép thực thi logic Service bên trong
        Mockito.when(txManagerMock.query(Mockito.any())).thenAnswer(invocation -> {
            Object lambdaCallback = invocation.getArgument(0);
            java.lang.reflect.Method method = lambdaCallback.getClass().getDeclaredMethods()[0];
            method.setAccessible(true);
            return method.invoke(lambdaCallback, Mockito.mock(java.sql.Connection.class));
        });

        // Giả lập PasswordHashing trả về true khi khớp mật khẩu
        passwordHashingMockedStatic.when(() -> PasswordHashing.checkPassword("raw_password", "hashed_password")).thenReturn(true);

        // Chạy hàm thực tế
        User result = authService.authenticate("testuser", "raw_password");

        // Khẳng định kết quả
        assertNotNull(result);
        assertEquals(mockUser, result);

        // Khẳng định block Lambda chạy thành công và đã xóa password
        Mockito.verify(mockUser, Mockito.times(1)).setPassword(null);
    }

    @Test
    void testAuthenticate_AccountBanned_ThrowsAuthException() {
        // Giả lập txManager ném thẳng ra ngoại lệ AuthException khi chạy block code bên trong do tài khoản bị BAN
        Mockito.when(txManagerMock.query(Mockito.any())).thenThrow(new AuthException("Your account has been BANNED."));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.authenticate("banned_user", "password");
        });

        assertEquals("Your account has been BANNED.", exception.getMessage());
    }

    @Test
    void testAuthenticate_InvalidCredentials_ThrowsAuthException() {
        // Giả lập trường hợp sai tài khoản/mật khẩu
        Mockito.when(txManagerMock.query(Mockito.any())).thenThrow(new AuthException("Invalid account name or password"));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.authenticate("wrong_user", "wrong_pass");
        });

        assertEquals("Invalid account name or password", exception.getMessage());
    }

    // ====================================================================
    // 2. TEST CASE CHO HÀM: register (Đăng ký)
    // ====================================================================

    @Test
    void testRegister_Success() {
        // run() trả về void nên dùng doNothing để giả lập chạy qua mượt mà
        Mockito.doNothing().when(txManagerMock).run(Mockito.any());
        passwordHashingMockedStatic.when(() -> PasswordHashing.hashPassword("pass")).thenReturn("hashed");

        // Đăng ký không ném ra exception nào nghĩa là thành công
        assertDoesNotThrow(() -> authService.register("newuser", "pass", "email@test.com"));

        Mockito.verify(txManagerMock, Mockito.times(1)).run(Mockito.any());
    }

    @Test
    void testRegister_UsernameExists_ThrowsValidationException() {
        // Giả lập logic trùng tên tài khoản ném lỗi
        Mockito.doThrow(new ValidationException("Account name 'existing_user' already exists."))
                .when(txManagerMock).run(Mockito.any());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.register("existing_user", "pass", "email@test.com");
        });

        assertEquals("Account name 'existing_user' already exists.", exception.getMessage());
    }

    // ====================================================================
    // 3. TEST CASE CHO HÀM: canAccess (Phân quyền hành động)
    // ====================================================================

    @Test
    void testCanAccess_PublicActions_AlwaysTrue() {
        // LOGIN, SIGNUP, PING không cần truyền đối tượng user vẫn phải được phép qua cửa
        assertTrue(authService.canAccess(MessageType.LOGIN, null));
        assertTrue(authService.canAccess(MessageType.SIGNUP, null));
    }

    @Test
    void testCanAccess_UserNull_ReturnsFalse() {
        // Các quyền khác nếu truyền user null thì cấm luôn
        assertFalse(authService.canAccess(MessageType.BID_PLACE, null));
    }

    @Test
    void testCanAccess_AdminActions_SuccessForAdmin_FailForMember() {
        User adminMock = Mockito.mock(User.class);
        Mockito.when(adminMock.getRole()).thenReturn(UserRole.ADMIN);

        User memberMock = Mockito.mock(User.class);
        Mockito.when(memberMock.getRole()).thenReturn(UserRole.MEMBER);

        // Chức năng của ADMIN: Admin vào được, Member bị chặn
        assertTrue(authService.canAccess(MessageType.ADMIN_BAN_USER, adminMock));
        assertFalse(authService.canAccess(MessageType.ADMIN_BAN_USER, memberMock));
    }

    @Test
    void testCanAccess_MemberActions_SuccessForMember_FailForAdmin() {
        User adminMock = Mockito.mock(User.class);
        Mockito.when(adminMock.getRole()).thenReturn(UserRole.ADMIN);

        User memberMock = Mockito.mock(User.class);
        Mockito.when(memberMock.getRole()).thenReturn(UserRole.MEMBER);

        // Chức năng đặt giá (BID_PLACE): Member vào được, Admin bị chặn
        assertTrue(authService.canAccess(MessageType.BID_PLACE, memberMock));
        assertFalse(authService.canAccess(MessageType.BID_PLACE, adminMock));
    }
}