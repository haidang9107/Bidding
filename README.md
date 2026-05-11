# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-FF0000?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

## 📖 Tổng quan dự án (Overview)

Đây là hệ thống đấu giá trực tuyến được xây dựng thuần túy dựa trên kiến trúc **Client-Server**, truyền tải dữ liệu qua **Socket** và định dạng **JSON**. Dự án sử dụng ngôn ngữ **Java 25** hiện đại cùng **JavaFX 21.0.6** cho giao diện đồ họa (GUI) phía Client. Phía Server được xây dựng bằng Java Core xử lý đa luồng kết hợp cơ sở dữ liệu.

Mục tiêu của dự án là tạo ra một nền tảng minh bạch, an toàn và thời gian thực (real-time) cho phép người bán đăng tải sản phẩm và người mua tham gia trả giá một cách công bằng.

> **Lưu ý:** Hiện tại dự án đang sử dụng kiến trúc Java Core thuần túy kết hợp Socket để xử lý giao tiếp mạng, qua đó nắm vững các kiến thức nền tảng về đa luồng (Multi-threading) và Design Patterns. (Có thể cân nhắc tích hợp các Framework mở rộng sau này).

---

## 🚀 Tổng quan Công nghệ (Tech Stack)

*   **Java 25 (LTS):** Phiên bản Java mới nhất, hỗ trợ các tính năng ngôn ngữ hiện đại.
*   **JavaFX 21.0.6:** Thư viện đồ họa hiện đại để xây dựng giao diện người dùng (Client).
*   **Maven 3.13.0:** Công cụ quản lý dự án, build và quản lý dependencies.
*   **Socket (TCP/IP):** Giao thức kết nối thời gian thực giữa Client và Server (Sử dụng ServerSocket và Socket của Java Core).
*   **GSON 2.11.0:** Thư viện chuyển đổi đối tượng Java sang chuỗi JSON và ngược lại.
*   **Database:** MySQL / PostgreSQL / SQLite qua JDBC.

---

## 🏗 Cấu trúc dự án (Multi-Module Architecture)

Dự án được triển khai theo mô hình **Maven Multi-Module** để đảm bảo tính nhất quán dữ liệu và dễ dàng bảo trì:

### 1. 📦 `common` (Shared Library)
"Từ điển chung" chứa các tài nguyên dùng cho cả Client và Server (Các đối tượng dữ liệu, cấu trúc gói tin mạng). Bất kỳ thay đổi nào ở đây sẽ ảnh hưởng đến cả hai phía.
- Chi tiết: [common/README.md](./common/README.md)

### 2. 🖥️ `server` (Backend)
Bộ não xử lý logic nghiệp vụ đấu giá, quản lý Database và kết nối Socket đa luồng (Broadcast).
- Chi tiết: [server/README.md](./server/README.md)

### 3. 🎨 `client` (Frontend)
Giao diện người dùng JavaFX, xử lý tương tác người dùng và lắng nghe phản hồi thời gian thực từ Server.
- Chi tiết: [client/README.md](./client/README.md)

---

## 📡 Giao thức truyền thông (Communication Protocol)

Mọi dữ liệu trao đổi giữa Client và Server đều được chuẩn hóa dưới dạng **JSON**. Tuyệt đối không gửi String thô. Mọi gói tin phải được đóng gói qua Object trong module `common`.

**Ví dụ về một gói tin Gửi giá thầu (Bid Place):**

*Request (từ Client gửi đi):*
```json
{
  "type": "BID_PLACE",
  "payload": "{\"productId\": 101, \"amount\": 500.0}",
  "timestamp": 1715498400000
}
```

*Response (từ Server trả về):*
```json
{
  "type": "SUCCESS",
  "success": true,
  "message": "Đặt giá thành công!",
  "data": "{\"newHighestBid\": 500.0}"
}
```

---

## 🛠 Hướng dẫn cài đặt và chạy ứng dụng

### Yêu cầu hệ thống:
*   **JDK 25** trở lên.
*   **Maven 3.9+**.
*   **PostgreSQL / MySQL** (hoặc dùng SQLite cấu hình sẵn).

### Các bước thực hiện:

1.  **Build toàn bộ dự án:**
    ```bash
    mvn clean install
    ```

2.  **Chạy Server:**
    Chạy class main `ServerApp.java` trong module `server`. Hoặc dùng lệnh:
    ```bash
    mvn -pl server exec:java -Dexec.mainClass="org.example.server.ServerApp"
    ```

3.  **Chạy Client:**
    Chạy class main `ClientApp.java` trong module `client`. Hoặc dùng lệnh:
    ```bash
    mvn -pl client javafx:run
    ```
    *Lưu ý: Có thể mở nhiều process Client để kiểm thử tính năng đa luồng.*

---

## 📖 Giải thích Thuật ngữ chuyên ngành (Glossary)

Dưới đây là các thuật ngữ quan trọng, "luật chơi" và các khái niệm kỹ thuật được sử dụng xuyên suốt dự án:

1.  **Socket:** Một "cổng" kết nối cho phép hai ứng dụng (Client và Server) giao tiếp với nhau qua mạng liên tục.
2.  **TCP/IP:** Giao thức truyền tin đảm bảo dữ liệu gửi đi được nhận đầy đủ, an toàn và đúng thứ tự.
3.  **JSON (JavaScript Object Notation):** Định dạng dữ liệu văn bản nhẹ, dễ đọc cho cả người và máy, dùng làm chuẩn để trao đổi dữ liệu.
4.  **POJO (Plain Old Java Object) / DTO (Data Transfer Object):** Các đối tượng Java đơn giản chỉ chứa các thuộc tính (fields) và các phương thức getter/setter, dùng để biểu diễn dữ liệu và truyền tải giữa các tầng.
5.  **Multi-module:** Cách tổ chức dự án thành nhiều phần nhỏ (module `common`, `server`, `client`), giúp quản lý code dễ dàng và tái sử dụng mã nguồn.
6.  **Payload:** Phần dữ liệu chính chứa thông tin nghiệp vụ được gửi đi trong một gói tin Request/Response.
7.  **Thread (Luồng):** Một đơn vị thực thi nhỏ nhất. Server sử dụng Multi-threading để tạo ra nhiều `ClientHandler` phục vụ nhiều Client cùng lúc.
8.  **FXML:** Ngôn ngữ đánh dấu XML của JavaFX dùng để định nghĩa giao diện người dùng, tách biệt logic (Controller) khỏi thiết kế (View).
9.  **DAO (Data Access Object) / Repository:** Tầng tương tác trực tiếp với Database để thao tác CRUD (Create, Read, Update, Delete). Chỉ Server mới được truy cập DAO.
10. **Singleton Pattern:** Mẫu thiết kế đảm bảo một lớp chỉ có một thể thể (instance) duy nhất (ví dụ: trình quản lý kết nối Socket).
11. **Platform.runLater():** Kỹ thuật đặc thù của JavaFX để đưa các thao tác cập nhật giao diện (từ dữ liệu nhận qua Socket) về lại UI Thread chính một cách an toàn. Tuyệt đối không gọi `socket.read()` gây block trên UI Thread.
12. **Auto-Bidding (Đấu giá tự động):** Người dùng cài đặt giá tối đa (`maxBid`) và bước giá (`increment`). Hệ thống sẽ tự động trả giá cao hơn đối thủ dựa trên bước giá, nhưng không vượt mức tối đa.
13. **Anti-sniping (Chống bắn tỉa):** Thuật toán tự động gia hạn thời gian phiên đấu giá nếu có người đặt giá ở những giây cuối cùng.
14. **Concurrent Bidding (Đấu giá đồng thời):** Xử lý an toàn khi nhiều Bidder cùng đặt giá tại một mili-giây, ngăn chặn lỗi "Lost Update".
