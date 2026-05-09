# FinalProject: Bidding

# 🏗️ BIDDING PROJECT STRUCTURE - HƯỚNG DẪN CẤU TRÚC DỰ ÁN

Tài liệu này quy định cấu trúc thư mục và trách nhiệm của từng module trong dự án Đấu giá (Bidding) sử dụng JavaFX, Socket và JSON.

---

## 1. Tổng quan kiến trúc (Multi-Module)
Dự án được triển khai theo mô hình **Maven Multi-Module** để đảm bảo tính nhất quán của dữ liệu:
* **`common`**: "Từ điển chung" chứa các đối tượng dữ liệu dùng cho cả Client và Server.
* **`server`**: Xử lý logic nghiệp vụ, quản lý Database và kết nối Socket (Broadcast).
* **`client`**: Giao diện người dùng JavaFX và xử lý phản hồi từ Server.

---

## 2. Chi tiết cấu trúc thư mục

### 📂 `Bidding/` (Root)
* `pom.xml`: Quản lý phiên bản chung cho toàn bộ dự án (JavaFX, Jackson/Gson, MySQL Driver).
* `README.md`: Hướng dẫn thiết lập môi trường cho thành viên mới.

#### 📂 `common/` (Shared Module)
*Nơi chứa mã nguồn dùng chung. Thay đổi tại đây sẽ cập nhật cho cả Server và Client.*
* `src/main/java/org/example/common/`
    * `model/`: Các thực thể dữ liệu (VD: `User.java`, `Product.java`, `Bid.java`).
    * `payload/`:
        * `MessageType.java`: **Enum** quy định loại lệnh (VD: `BID_PLACE`, `BID_UPDATE`, `TIMER_TICK`, `ERROR`).
        * `Response.java`: Cấu trúc gói tin phản hồi chuẩn từ Server.
    * `util/`: Các công cụ hỗ trợ như `JsonConverter.java` (Sử dụng Jackson/Gson).

#### 📂 `server/` (Backend Module)
*Nơi quản lý toàn bộ "luật chơi" của phiên đấu giá.*
* `src/main/java/org/example/server/`
    * `network/`:
        * `SocketServer.java`: Khởi tạo ServerSocket, chấp nhận kết nối.
        * `ClientHandler.java`: Luồng xử lý tin nhắn JSON cho từng Client cụ thể.
        * `Broadcaster.java`: Quản lý danh sách kết nối và gửi thông tin mới cho tất cả Client.
    * `controller/`: Điều khiển logic server.
    * `service/`:
        * `AuctionService.java`: Logic kiểm tra giá, tính toán thời gian kết thúc.
    * `repository/` (**DAO**): Chứa code SQL để làm việc với Database thông qua Entity.
    * `exception/`: Xử lý các lỗi nghiệp vụ (VD: `InvalidBidException`).
* `src/main/resources/`: Cấu hình hệ thống (`db.properties`).

#### 📂 `client/` (Frontend Module)
*Giao diện JavaFX và tương tác người dùng.*
* `src/main/java/org/example/client/`
    * `network/`:
        * `SocketClient.java`: Kết nối tới Server.
        * `ServerListener.java`: Luồng chạy ngầm liên tục lắng nghe JSON từ Server gửi về.
    * `controller/`: Điều khiển logic giao diện (Xử lý sự kiện nút bấm).
    * `view/` (hoặc util): Chứa các class hỗ trợ hiển thị.
* `src/main/resources/`:
    * `view/`: Chứa các file `.fxml` được phân loại (VD: `login.fxml`, `main_auction.fxml`).
    * `css/`: Định dạng giao diện người dùng.

---

## 3. Quy tắc phát triển (Workflow)

Để đảm bảo dự án không bị lỗi khi ghép nối, các thành viên cần tuân thủ:

1.  **Giao tiếp qua JSON:** Tuyệt đối không gửi String thô. Mọi gói tin phải được đóng gói qua Object trong module `common`.
2.  **Nguyên tắc Threading:** * Client không bao giờ thực hiện `socket.read()` trên UI Thread.
    * Sử dụng `Platform.runLater()` khi muốn cập nhật giao diện từ dữ liệu nhận được qua Socket.
3.  **Database:** Chỉ Server mới được phép truy cập Database. Client lấy dữ liệu thông qua Server.

---

## 4. Danh sách thư viện khuyến nghị (Dependencies)
* **JSON:** Jackson Databind hoặc Google Gson.
* **Database:** MySQL Connector/J.
* **UI:** JavaFX Controls & FXML.
* **Testing:** JUnit 5.