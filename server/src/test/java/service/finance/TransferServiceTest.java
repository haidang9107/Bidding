package service.finance;

import org.example.dto.response.PagedResponse;
import org.example.model.Transaction;
import org.example.server.repository.TransactionManager;
import org.example.server.service.finance.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionManager txManagerMock;
    private TransactionService transactionService;

    // Chạy trước mỗi hàm @Test để khởi tạo môi trường sạch
    @BeforeEach
    void setUp() {
        // 1. Arrange: Tạo Mock cho TransactionManager
        txManagerMock = Mockito.mock(TransactionManager.class);
        // 2. Truyền Mock vào Service cần kiểm thử
        transactionService = new TransactionService(txManagerMock);
    }

    // ====================================================================
    // 1. KỊCH BẢN: Lấy lịch sử giao dịch phân trang thành công
    // ====================================================================
    @Test
    void testGetTransactionsPaged_Success() {
        // 1. Arrange: Chuẩn bị dữ liệu giả lập trả về
        Transaction mockTx1 = Mockito.mock(Transaction.class);
        Transaction mockTx2 = Mockito.mock(Transaction.class);
        List<Transaction> mockList = Arrays.asList(mockTx1, mockTx2);

        PagedResponse<Transaction> expectedResponse = new PagedResponse<>(mockList, 2, 1, 10);

        // Định nghĩa: Khi txManager.query được gọi với bất kỳ lambda nào, hãy trả về expectedResponse
        Mockito.when(txManagerMock.query(Mockito.any())).thenReturn(expectedResponse);

        // 2. Act: Gọi hàm thực tế ở tầng Service
        PagedResponse<Transaction> actualResponse = transactionService.getTransactionsPaged("test_user", 1, 10);

        // 3. Assert: Kiểm tra xem kết quả có đúng như mong đợi không
        assertNotNull(actualResponse, "Kết quả trả về không được phép null");
        assertEquals(2, actualResponse.getTotalItems(), "Tổng số item phải bằng 2");
        assertEquals(1, actualResponse.getCurrentPage(), "Trang hiện tại phải là 1");
        assertEquals(10, actualResponse.getPageSize(), "Kích thước trang phải là 10");
        assertEquals(mockList, actualResponse.getItems(), "Danh sách transaction trả về phải khớp");

        // Xác minh xem txManager thực sự đã chạy qua lệnh query đúng 1 lần
        Mockito.verify(txManagerMock, Mockito.times(1)).query(Mockito.any());
    }

    // ====================================================================
    // 2. KỊCH BẢN: Lấy toàn bộ lịch sử giao dịch (Không phân trang) thành công
    // ====================================================================
    @Test
    void testGetTransactions_Success() {
        // 1. Arrange: Chuẩn bị danh sách giả lập
        Transaction mockTx = Mockito.mock(Transaction.class);
        List<Transaction> expectedList = Arrays.asList(mockTx);

        // Định nghĩa hành vi cho Mock
        Mockito.when(txManagerMock.query(Mockito.any())).thenReturn(expectedList);

        // 2. Act: Gọi hàm
        List<Transaction> actualList = transactionService.getTransactions("test_user");

        // 3. Assert: Khẳng định kết quả
        assertNotNull(actualList);
        assertEquals(1, actualList.size(), "Danh sách trả về phải có đúng 1 phần tử");
        assertEquals(expectedList, actualList);

        Mockito.verify(txManagerMock, Mockito.times(1)).query(Mockito.any());
    }
}