# Module: Server (Backend)

Module `server` là bộ khung của dự án, quản lý toàn bộ "luật chơi" của các phiên đấu giá. Module này được xây dựng thuần túy bằng **Java Core**, đảm nhận việc kết nối Socket đa luồng, xử lý logic đấu giá an toàn, lưu trữ dữ liệu vào Database và phát sóng (broadcast) thông tin thời gian thực đến Client.

## 📂 Cấu trúc logic nghiệp vụ (Folder Structure Logic)

```text
server/src/main/java/org/example/server/
├── network/                 # Quản lý tầng giao tiếp mạng (Socket), mở cổng chờ kết nối, cấp phát luồng cho từng Client và phát sóng tín hiệu.
├── controller/              # Tầng điều phối, đóng vai trò như router nhận các Request đã phân tích từ luồng mạng và gọi đúng logic nghiệp vụ.
├── service/                 # Tầng chứa toàn bộ logic "luật chơi" phức tạp nhất của dự án (tính toán giá, kiểm tra thời gian, auto-bid).
├── repository/              # Tầng truy xuất dữ liệu (DAO), chịu trách nhiệm duy nhất cho việc thực thi các câu lệnh SQL để đọc/ghi vào Database.
└── exception/               # Chứa định nghĩa các lỗi nghiệp vụ tùy chỉnh của hệ thống để trả về phản hồi lỗi cụ thể cho Client.
```

## 🛠 Nguyên tắc hoạt động & Luồng xử lý

### 1. Tầng `network/` (Socket & Broadcasting)
- **Lắng nghe kết nối**: Server luôn duy trì một cổng mở để chờ các Client kết nối tới.
- **Xử lý đa luồng (Multi-threading)**: Mỗi khi có một Client tham gia, Server tạo ra một luồng (Thread) hoàn toàn độc lập để liên tục lắng nghe tin nhắn từ Client đó, đảm bảo các Client không bị chặn (block) lẫn nhau.
- **Phát sóng (Broadcast)**: Khi có một thay đổi lớn (giá thầu mới hợp lệ), hệ thống sẽ dùng cơ chế Broadcast để đẩy tin nhắn thông báo dạng JSON tới toàn bộ các Client đang tham gia phòng đấu giá đó cùng lúc.

### 2. Tầng `controller/` & `service/` (Nghiệp vụ cốt lõi)
- **Controller** phân tích yêu cầu từ gói tin mạng và điều hướng luồng xử lý.
- **Service** xử lý các bài toán kỹ thuật phức tạp:
  - **Concurrent Bidding**: Sử dụng cơ chế khóa (Locks/Synchronized) để ngăn tình trạng nhiều người đặt giá cùng một mili-giây dẫn đến sai lệch dữ liệu.
  - **Anti-sniping**: Thuật toán tự động cộng thêm giây nếu phát hiện lượt đặt giá sát giờ đóng cửa.

### 3. Tầng `repository/` (Database Security)
- Hệ thống tuân thủ nguyên tắc bảo mật: Client KHÔNG BAO GIỜ được kết nối thẳng vào Database.
- Tầng `repository` thao tác với Database (qua JDBC/DAO) để lưu trữ và trích xuất dữ liệu, sau đó Server sẽ chuyển đổi thành dạng DTO chuẩn trước khi gửi xuống Client.
