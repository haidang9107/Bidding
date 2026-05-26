package org.example.server.service.auth;

import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.model.user.Admin;
import org.example.model.user.Member;
import org.example.model.user.User;
import org.example.server.exception.AuthException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.user.auth.AuthService;
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

        lenient().doAnswer(inv -> {
            TransactionManager.TransactionalWork<?> fn = inv.getArgument(0);
            return fn.execute(connection);
        }).when(txManager).execute(any());

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
            // Tạo member với password đã hash bằng BCrypt
            String raw = "password123";
            String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(raw, org.mindrot.jbcrypt.BCrypt.gensalt());
            Member m = new Member("alice", hashed, "alice@test.com", null, 0, 0, 0);

            when(userDao.findByAccountname(connection, "alice")).thenReturn(m);

            User result = authService.authenticate("alice", raw);

            assertNotNull(result);
            assertNull(result.getPassword(), "Password phải được xóa sau authenticate");
        }

        @Test
        @DisplayName("TC-AUTH-02: sai mật khẩu ném AuthException")
        void wrongPassword_throwsAuth() throws Exception {
            String hashed = org.mindrot.jbcrypt.BCrypt.hashpw("correct", org.mindrot.jbcrypt.BCrypt.gensalt());
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
            String hashed = org.mindrot.jbcrypt.BCrypt.hashpw("pw", org.mindrot.jbcrypt.BCrypt.gensalt());
            Member m = new Member("alice", hashed, "a@t.com", null, 1, 0, 0); // status=1 = banned
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

    // ── canAccess ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canAccess()")
    class CanAccess {

        private Member member() {
            return new Member("u", "h", "u@t.com", null, 0, 0, 0);
        }

        private Admin admin() {
            return new Admin("adm", "h", "adm@t.com", null, 0);
        }

        @Test
        @DisplayName("TC-ACC-01: LOGIN/SIGNUP/PING không cần đăng nhập")
        void publicEndpoints_alwaysGranted() {
            assertTrue(authService.canAccess(MessageType.LOGIN,  null));
            assertTrue(authService.canAccess(MessageType.SIGNUP, null));
            assertTrue(authService.canAccess(MessageType.PING,   null));
        }

        @Test
        @DisplayName("TC-ACC-02: user null bị từ chối với endpoint yêu cầu đăng nhập")
        void nullUser_denied() {
            assertFalse(authService.canAccess(MessageType.BID_PLACE, null));
        }

        @Test
        @DisplayName("TC-ACC-03: MEMBER được phép BID_PLACE")
        void member_canBid() {
            assertTrue(authService.canAccess(MessageType.BID_PLACE, member()));
        }

        @Test
        @DisplayName("TC-ACC-04: MEMBER không được phép endpoint ADMIN")
        void member_cannotAdminBan() {
            assertFalse(authService.canAccess(MessageType.ADMIN_BAN_USER, member()));
        }

        @Test
        @DisplayName("TC-ACC-05: ADMIN được phép ADMIN_BAN_USER")
        void admin_canBanUser() {
            assertTrue(authService.canAccess(MessageType.ADMIN_BAN_USER, admin()));
        }

        @Test
        @DisplayName("TC-ACC-06: ADMIN không được phép BID_PLACE")
        void admin_cannotBid() {
            assertFalse(authService.canAccess(MessageType.BID_PLACE, admin()));
        }
    }

    // ── utility ───────────────────────────────────────────────────────────

    private static void setField(Object obj, String name, Object val) throws Exception {
        var f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
