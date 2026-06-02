package service.user;

import org.example.server.repository.TransactionManager;
import org.example.server.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private TransactionManager txManagerMock;
    private UserService userService;

    @BeforeEach
    void setUp() {
        txManagerMock = Mockito.mock(TransactionManager.class);
        userService = new UserService(txManagerMock);
    }

    @Test
    void testUpdateAvatar_ReturnsTrue() {
        // Cấu hình thẳng thừng: Cứ gọi execute với bất kỳ lambda nào thì trả về true
        Mockito.when(txManagerMock.execute(Mockito.any())).thenReturn(true);

        boolean result = userService.updateAvatar("test_user", "/path/avatar.png");

        // Kiểm tra kết quả
        assertTrue(result);

        // Xác minh xem txManager thực sự đã được gọi đúng 1 lần
        Mockito.verify(txManagerMock, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    void testUpdateEmail_ReturnsTrue() {
        // Áp dụng tương tự cho hàm update email
        Mockito.when(txManagerMock.execute(Mockito.any())).thenReturn(true);

        boolean result = userService.updateEmail("test_user", "test@example.com");

        assertTrue(result);

        Mockito.verify(txManagerMock, Mockito.times(1)).execute(Mockito.any());
    }
}