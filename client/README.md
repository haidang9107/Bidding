# Module: Client (Frontend)

Module `client` cung cấp giao diện đồ họa cho dự án, được xây dựng bằng **JavaFX 21.0.6**. Hệ thống tuân thủ chặt chẽ mô hình **MVC** (Model - View - Controller), kết hợp kiến trúc Socket chạy ngầm để nhận các cập nhật trực tiếp (real-time) từ Server một cách mượt mà.

## 📂 Cấu trúc logic nghiệp vụ (Folder Structure Logic)

```text
client/src/main/java/org/example/client/
├── network/                 # Xử lý giao tiếp với Server qua Socket, duy trì luồng nền liên tục lắng nghe dữ liệu JSON trả về.
├── controller/              # Tầng điều khiển, gắn kết các sự kiện từ người dùng (click chuột, nhập phím) với logic xử lý dữ liệu và cập nhật giao diện.
└── view/                    # Tầng điều hướng, cung cấp các công cụ quản lý việc chuyển đổi qua lại giữa các màn hình (Scenes) của ứng dụng.

client/src/main/resources/
├── view/                    # Chứa các file FXML, nơi thiết kế thuần túy các thành phần hiển thị trên màn hình.
└── css/                     # Chứa các file định dạng màu sắc, kích thước, hiệu ứng (Stylesheets).
```

## 🛠 Nguyên tắc phát triển & Workflow

### 1. Phân tách Giao diện và Logic (MVC)
- Giao diện (giao diện, nút bấm, bảng biểu) hoàn toàn được định nghĩa riêng biệt trong các file `.fxml` tại thư mục `resources/view`.
- Các hành động của người dùng (ví dụ: ấn nút "Đặt giá") được gắn (binding) với các logic mã trong thư mục `controller/`. 
- Khi có sự kiện, Controller sẽ thu thập dữ liệu trên màn hình, đóng gói thành đối tượng `Request` và yêu cầu tầng `network` gửi qua Socket đi.

### 2. Lắng nghe Real-time (Luồng mạng chạy ngầm)
- Trong tầng `network/`, hệ thống duy trì một Thread (luồng) độc lập liên tục túc trực để đọc dữ liệu trả về từ Server (Observer pattern thụ động).
- Cơ chế này cho phép Client nhận các sự kiện như "Có người vừa đặt giá mới" ngay lập tức mà không cần người dùng phải bấm nút tải lại trang (refresh).

### 3. Nguyên tắc Threading (CỰC KỲ QUAN TRỌNG)
- **Tuyệt đối không chặn UI Thread:** Client không bao giờ được phép thực hiện các thao tác chờ mạng (như `socket.read()`) trên UI Thread (luồng chính làm nhiệm vụ vẽ giao diện). Việc này sẽ làm ứng dụng bị "treo" (Not Responding).
- **Sử dụng `Platform.runLater()`:** Khi luồng mạng chạy ngầm nhận được tín hiệu cần cập nhật giao diện, nó **KHÔNG ĐƯỢC** phép tự ý thay đổi giao diện (ví dụ: thay đổi Text của một Label). Thay vào đó, mọi lệnh cập nhật giao diện phải được bọc trong hàm `Platform.runLater(...)` để đẩy công việc đó về lại luồng UI chính xử lý một cách an toàn.
