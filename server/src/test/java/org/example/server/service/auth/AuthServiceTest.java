package org.example.server.service.auth;

import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.AuthException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.user.auth.PasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TransactionManager txManager;
    @Mock private UserDao            userDao;
    @Mock private Connection         connection;

    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(txManager);
        setField(authService, "userDao", userDao);

        // ĐỒNG BỘ MOCK TXMANAGER: Giả lập chính xác hành vi chạy lambda của txManager
        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(txManager).query(any());

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalRunnable fn = inv.getArgument(0);
            fn.execute(connection);
            return null;
        }).when(txManager).run(any());
    }

    // ── authenticate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("authenticate()")
    class Authenticate {

        @Test
        @DisplayName("TC-AUTH-01: đăng nhập thành công – password bị xóa")
        void success_passwordCleared() throws Exception {
            String raw = "password123";
            String hashed = PasswordHashing.hashPassword(raw);
            Member m = new Member("alice", hashed, "alice@test.com", null, 0, 0, 0);

            when(userDao.findByAccountname(connection, "alice")).thenReturn(m);

            User result = authService.authenticate("alice", raw);

            assertNotNull(result);
            assertNull(result.getPassword(), "Password phải được xóa sau authenticate");
        }

        @Test
        @DisplayName("TC-AUTH-02: sai mật khẩu ném AuthException")
        void wrongPassword_throwsAuth() throws Exception {
            String hashed = PasswordHashing.hashPassword("correct");
            Member m = new Member("alice", hashed, "a@t.com", null, 0, 0, 0);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(m);

            assertThrows(AuthException.class,
                    () -> authService.authenticate("alice", "wrong"));
        }

        @Test
        @DisplayName("TC-AUTH-03: tài khoản không tồn tại ném AuthException")
        void unknownAccount_throwsAuth() throws Exception {
            when(userDao.findByAccountname(connection, "ghost")).thenReturn(null);

            assertThrows(AuthException.class,
                    () -> authService.authenticate("ghost", "any"));
        }

        @Test
        @DisplayName("TC-AUTH-04: tài khoản bị ban (status=1) ném AuthException")
        void bannedAccount_throwsAuth() throws Exception {
            String hashed = PasswordHashing.hashPassword("pw");
            Member m = new Member("alice", hashed, "a@t.com", null, 1, 0, 0);
            when(userDao.findByAccountname(connection, "alice")).thenReturn(m);

            assertThrows(AuthException.class,
                    () -> authService.authenticate("alice", "pw"));
        }
    }

    // ── register ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("TC-REG-01: đăng ký thành công")
        void success_userCreated() throws Exception {
            when(userDao.findByAccountname(connection, "bob")).thenReturn(null);
            when(userDao.createUser(eq(connection), any(User.class))).thenReturn(true);

            assertDoesNotThrow(() ->
                    authService.register("bob", "pass123", "bob@test.com"));

            verify(userDao).createUser(eq(connection), any(Member.class));
        }

        @Test
        @DisplayName("TC-REG-02: accountname đã tồn tại ném ValidationException")
        void duplicateAccount_throwsValidation() throws Exception {
            Member existing = new Member("bob", "h", "b@t.com", null, 0, 0, 0);
            when(userDao.findByAccountname(connection, "bob")).thenReturn(existing);

            assertThrows(ValidationException.class,
                    () -> authService.register("bob", "pass", "bob@test.com"));
        }

        @Test
        @DisplayName("TC-REG-03: DB thất bại ném AuthException")
        void dbFailure_throwsAuth() throws Exception {
            when(userDao.findByAccountname(connection, "carol")).thenReturn(null);
            when(userDao.createUser(eq(connection), any())).thenReturn(false);

            assertThrows(AuthException.class,
                    () -> authService.register("carol", "pw", "c@test.com"));
        }
    }

    private static void setField(Object obj, String name, Object val) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }
}