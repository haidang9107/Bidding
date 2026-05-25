# 🎨 Module: Client (Frontend GUI)

Module `client` đảm nhận phần giao diện tương tác người dùng của hệ thống đấu giá. Hệ thống được phát triển bằng **JavaFX 21.0.6** và tuân thủ chặt chẽ kiến trúc **MVC** (Model-View-Controller) cùng với việc quản lý Socket chạy ngầm an toàn.

---

## 📂 Architecture Logic (Cấu trúc thư mục)

```text
client/src/main/java/org/example/client/
├── ClientApp.java           # Lớp Main của JavaFX khởi chạy giao diện.
├── ClientLauncher.java      # Lớp Launcher bao bọc ClientApp (tránh lỗi module khi chạy JAR).
├── component/               # Các UI components tái sử dụng (VD: ProductCard.java).
├── controller/              # Các Controller của FXML xử lý sự kiện người dùng (Click, Scroll...).
├── dialog/                  # Cửa sổ Pop-up (VD: Mở nạp tiền, Đặt giá).
├── network/                 # Xử lý mạng (SocketClient, ServerListener) kết nối đến Server.
├── notification/            # Trình quản lý thông báo, toast messages trên màn hình.
├── session/                 # Trình quản lý phiên làm việc hiện tại của người dùng (chứa Token, thông tin cá nhân).
├── util/                    # Trình quản lý Scene Router chuyển đổi màn hình linh hoạt.
└── watchlist/               # Trình quản lý danh sách sản phẩm yêu thích và các đấu giá đang theo dõi.

client/src/main/resources/
├── css/                     # Toàn bộ Stylesheets (CSS) thiết kế bố cục, màu sắc, hiệu ứng cho FXML.
├── images/                  # Tài nguyên tĩnh (Icons, hình nền, avatar mẫu...).
└── view/                    # Cấu trúc View được thiết kế bằng thẻ đánh dấu FXML.
```

---

## 🛠 Nguyên tắc phát triển & Workflow

### 1. Separation of Concerns (Phân tách logic theo MVC)
- **View (Giao diện):** Hoàn toàn sử dụng `.fxml` tại `resources/view`. Nếu cần style, sử dụng `.css`. Không code giao diện cứng trong file Java.
- **Controller (Điều khiển):** Các class trong `controller/` dùng `@FXML` để link tới giao diện. Controller sẽ bắt sự kiện của UI (nút bấm), lấy dữ liệu màn hình, gọi API Socket, và cập nhật lại màn hình khi có Response.

### 2. Kiến trúc Socket Client (Lắng nghe Real-time)
Để biến ứng dụng thành "thời gian thực" (Real-time), khi kết nối mạng được khởi tạo, hệ thống bật một **Background Thread** (`ServerListener.java`). Luồng này có vòng lặp vô hạn `while(true)` chuyên nhận chuỗi JSON do Server gửi tới.
- Khi có một gói tin tới (ví dụ: Ai đó vừa cập nhật giá thầu), `ServerListener` sẽ phân loại `MessageType` và chuyển qua cho `Controller` đang kích hoạt hoặc Trigger Notification Manager để bắn Pop-up nhỏ dưới góc màn hình.

### 3. Nguyên tắc Threading cực kỳ quan trọng (Platform.runLater)
Trong JavaFX, **chỉ có duy nhất JavaFX Application Thread (UI Thread) mới được quyền thay đổi các node trên màn hình (Label, Button, TextField...)**.
Do đó:
- **Nguyên tắc 1:** Tuyệt đối không được gọi `socket.read()` hay bất cứ lệnh nào block luồng trên các hàm sự kiện (như `onLoginClicked()`). Mọi thao tác chờ phản hồi Server phải thực hiện qua cơ chế callback hoặc listener ngầm.
- **Nguyên tắc 2:** Khi luồng `ServerListener` (luồng mạng) nhận được data từ Server và muốn cập nhật giao diện (VD: đổi text của thẻ giá), bắt buộc phải đưa thao tác đó vào `Platform.runLater()`:
  ```java
  // BẮT BUỘC KHI UPDATE GIAO DIỆN TỪ LUỒNG MẠNG
  Platform.runLater(() -> {
      currentPriceLabel.setText(newPrice + " VND");
  });
  ```

---

## 🚀 Hướng dẫn chạy ứng dụng

1. Đảm bảo Module `common` đã được cài đặt và Server đang chạy.
2. Từ thư mục `client`, chạy lệnh Maven:
   ```bash
   mvn clean javafx:run
   ```
*(Bạn có thể chạy song song nhiều terminal để mô phỏng nhiều người chơi đấu giá cùng lúc).*
