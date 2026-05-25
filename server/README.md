# 🖥️ Module: Server (Backend Core)

Module `server` là trái tim của hệ thống đấu giá. Được xây dựng thuần túy bằng **Java Core** (Không dùng các framework cồng kềnh như Spring Boot), nó xử lý toàn bộ logic phức tạp của đấu giá trực tuyến. Server vận hành một hệ thống **Socket TCP/IP** liên tục đa luồng, xử lý dữ liệu với **MySQL** qua HikariCP Connection Pool.

---

## 📂 Architecture Logic (Cấu trúc thư mục)

```text
server/src/main/java/org/example/server/
├── ServerApp.java           # Lớp khởi tạo chính, liên kết Database và mở cổng ServerSocket.
├── controller/              # Nhận các "Request" đã được phân tích và điều phối sang Service.
├── event/                   # Hệ thống Event Bus / Observer ngầm trong Server để xử lý decoupling.
├── exception/               # Các ngoại lệ kinh doanh (Business Exceptions) tự định nghĩa.
├── network/                 # Xử lý tầng TCP Socket (Server, ClientHandler, Broadcasting).
├── repository/              # Tầng DAO (Data Access Object) xử lý trực tiếp chuỗi SQL với MySQL.
└── service/                 # Tầng logic lõi (Luật giá thầu, Thời gian đấu giá, Auto-Bidding).

server/src/main/resources/
└── MySQL/                   # Các tệp kịch bản khởi tạo database ban đầu.
    ├── 1schema.sql          # Tạo các Table.
    └── 2data.sql            # Tạo dữ liệu Mock test (Admin, Users, Products).
```

---

## 🛠 Nguyên tắc hoạt động & Core Logic

### 1. Multi-threaded Socket Server
- **Khởi tạo:** `ServerSocket` liên tục lắng nghe tại cổng đã định (mặc định 8888).
- **Cấp phát luồng:** Mỗi khi có 1 Client báo kết nối, ServerApp tạo một `ClientHandler` (thừa kế `Thread` hoặc `Runnable`). Luồng này sẽ độc quyền nói chuyện với Client đó trong suốt phiên, đảm bảo hệ thống không bị nghẽn (Blocking IO).
- **Phát sóng (Broadcasting):** Có một danh sách tĩnh (Thread-safe List) chứa toàn bộ các `ClientHandler` đang hoạt động. Khi có người đặt giá thành công, Service gọi lệnh Broadcast để gửi cấu trúc gói JSON về toàn bộ các Client khác cùng lúc.

### 2. Cơ sở dữ liệu và Connection Pool
- **Không giữ kết nối Database lâu:** Thay vì mỗi Client mở 1 kết nối Database, Server sử dụng `HikariCP` làm hồ chứa kết nối (Connection Pool). Tầng `repository` (DAO) chỉ mượn Connection từ Pool khi cần thực thi SQL, rồi trả lại ngay lập tức.
- **Bảo mật:** Mọi truy vấn SQL đều phải dùng `PreparedStatement` để phòng tránh lỗi bảo mật SQL Injection.
- **Mã hóa:** Mật khẩu người dùng được băm thuật toán BCrypt trước khi lưu trữ xuống Database thông qua thư viện `jbcrypt`.

### 3. Logic Đấu Giá Tương Tranh (Concurrency Bidding)
- Khi 2 Client cùng gửi lệnh Bid một lúc cho cùng 1 sản phẩm, nếu không được thiết kế tốt sẽ dẫn tới "Lost Update" (Ghi đè dữ liệu sai).
- **Giải pháp:** Tầng `AuctionService` ứng dụng `ReentrantLock` hoặc khối `synchronized` dựa trên `Auction ID`. Ai tới trước sẽ được khóa tài nguyên xử lý trước, người tới sau vài phần ngàn giây sẽ thấy giá đã thay đổi và nhận phản hồi `Thất bại do giá đã bị người khác vượt`.

### 4. Hệ thống sự kiện (Event-Driven)
- Tầng `event/` cung cấp công cụ `EventPublisher`. Ví dụ: Khi người dùng gọi lệnh hủy đấu giá, thay vì gọi một đống hàm ở nhiều Service, Controller chỉ phát ra sự kiện `AuctionCancelledEvent`. Các Listener đã đăng ký sẽ tự động lắng nghe và làm nhiệm vụ của mình (VD: Trả lại tiền cọc, Thông báo Notification cho Client, Cập nhật trạng thái SQL).

---

## 🚀 Hướng dẫn cấu hình và chạy

### 1. Database Configurations
- Server cần kết nối đến MySQL. File thuộc tính kết nối sẽ lấy ưu tiên từ **Environment Variables** (Các file `.env`).
- Bạn có thể tùy chỉnh thông số kết nối ở file `.env` (tạo từ bản mẫu `.env.example`).
- Nếu dùng Docker Compose từ thư mục gốc, database sẽ tự khởi chạy ở port `3306`.

### 2. Run Server
Từ thư mục root của dự án, biên dịch và chạy bằng Maven Plugin:
```bash
mvn -pl server exec:java -Dexec.mainClass="org.example.server.ServerApp"
```
Server sẽ in log thông báo: `Server started on port 8888...`.
