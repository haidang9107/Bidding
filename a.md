## 🔨 Dự án Hệ thống Đấu giá Trực tuyến (Bidding System)

Chào mừng bạn đến với dự án **Bidding System**! Đây là một nền tảng đấu giá trực tuyến được xây dựng trên kiến trúc **Client-Server**, sử dụng ngôn ngữ lập trình **Java** hiện đại kết hợp với công nghệ truyền tải dữ liệu qua **Socket** và định dạng **JSON**.

---

## 🚀 Tổng quan Công nghệ (Tech Stack)

Dự án sử dụng các công nghệ tiên tiến nhất để đảm bảo hiệu năng và tính ổn định:

*   **Java 25 (LTS):** Phiên bản Java mới nhất, hỗ trợ các tính năng hiện đại.
*   **Spring Boot 4.0.6:** Khung làm việc mạnh mẽ cho phía Server (Backend).
*   **JavaFX 21.0.6:** Thư viện đồ họa hiện đại để xây dựng giao diện người dùng (Client).
*   **Maven:** Công cụ quản lý dự án và các thư viện phụ thuộc (Dependencies).
*   **Socket (TCP/IP):** Giao thức kết nối thời gian thực giữa Client và Server.
*   **GSON (2.11.0):** Thư viện của Google dùng để chuyển đổi đối tượng Java sang chuỗi JSON và ngược lại.

---

## 🏗️ Cấu trúc dự án (Multi-Module Architecture)

Dự án được chia thành 3 Module chính để tách biệt trách nhiệm:

### 1. 📦 `common` (Shared Library)
Module này chứa các tài nguyên dùng chung cho cả Client và Server. Bất kỳ thay đổi nào ở đây sẽ ảnh hưởng đến cả hai phía.
*   **`model/`**: Các thực thể dữ liệu (POJO) như `User`, `Product`, `Bid`.
*   **`payload/`**: Cấu trúc các gói tin trao đổi (`Request`, `Response`) và các loại thông điệp (`MessageType`).
*   **`util/`**: Công cụ hỗ trợ, nổi bật là `JsonConverter` để xử lý dữ liệu JSON.

### 2. 🖥️ `server` (Backend)
Đóng vai trò là "bộ não" điều khiển toàn bộ hệ thống.
*   **`network/`**: Quản lý kết nối Socket. Mỗi Client kết nối sẽ được xử lý bởi một `ClientHandler` riêng biệt trong một luồng (Thread) độc lập.
*   **`service/`**: Xử lý logic nghiệp vụ (ví dụ: kiểm tra giá thầu có hợp lệ không, ai là người thắng cuộc).
*   **`repository/`**: Tương tác với cơ sở dữ liệu (Database) để lưu trữ thông tin người dùng và sản phẩm.

### 3. 🎨 `client` (Frontend)
Giao diện đồ họa người dùng (GUI) giúp người tham gia đấu giá tương tác với hệ thống.
*   **`network/`**: Kết nối tới Server và lắng nghe các cập nhật giá thầu theo thời gian thực.
*   **`view/`**: Chứa các tệp FXML định nghĩa giao diện (Login, Auction Room).
*   **`controller/`**: Điều khiển logic của giao diện, xử lý các sự kiện click nút, nhập liệu.

---

## 📡 Giao thức truyền thông (Communication Protocol)

Mọi dữ liệu trao đổi giữa Client và Server đều được chuẩn hóa dưới dạng **JSON**.

### Ví dụ về một gói tin Gửi giá thầu (Bid Place):

**Request (từ Client gửi đi):**
```json
{
  "type": "BID_PLACE",
  "payload": "{\"productId\": 101, \"amount\": 500.0}",
  "timestamp": 1715498400000
}
```

**Response (từ Server trả về):**
```json
{
  "type": "SUCCESS",
  "success": true,
  "message": "Đặt giá thành công!",
  "data": "{\"newHighestBid\": 500.0}"
}
```

---

## 🛠️ Hướng dẫn cài đặt và chạy ứng dụng

### Yêu cầu hệ thống:
*   **JDK 25** trở lên.
*   **Maven 3.9+**.

### Các bước thực hiện:

1.  **Build toàn bộ dự án:**
    ```bash
    mvn clean install
    ```

2.  **Chạy Server:**
    Di chuyển vào thư mục `server/` và chạy:
    ```bash
    mvn spring-boot:run
    ```

3.  **Chạy Client:**
    Di chuyển vào thư mục `client/` và chạy:
    ```bash
    mvn javafx:run
    ```

---

## 📖 Giải thích Thuật ngữ (Glossary)

Dưới đây là một số thuật ngữ chuyên ngành được sử dụng trong dự án:

1.  **Socket:** Một "cổng" kết nối cho phép hai ứng dụng (Client và Server) giao tiếp với nhau qua mạng.
2.  **TCP/IP:** Giao thức truyền tin đảm bảo dữ liệu gửi đi được nhận đầy đủ và đúng thứ tự.
3.  **JSON (JavaScript Object Notation):** Định dạng dữ liệu văn bản nhẹ, dễ đọc cho cả người và máy, dùng để trao đổi dữ liệu.
4.  **POJO (Plain Old Java Object):** Các đối tượng Java đơn giản chỉ chứa các thuộc tính (fields) và các phương thức getter/setter, không phụ thuộc vào thư viện bên ngoài.
5.  **Multi-module:** Cách tổ chức dự án thành nhiều phần nhỏ (module), giúp quản lý code dễ dàng và tái sử dụng mã nguồn.
6.  **Payload:** Phần dữ liệu chính được gửi đi trong một gói tin (không bao gồm thông tin tiêu đề/hệ thống).
7.  **Thread (Luồng):** Một đơn vị thực thi nhỏ nhất của chương trình. Server sử dụng nhiều luồng để phục vụ nhiều Client cùng lúc (Multi-threading).
8.  **FXML:** Ngôn ngữ đánh dấu dựa trên XML dùng để định nghĩa giao diện người dùng trong JavaFX.
9.  **DAO (Data Access Object):** Đối tượng dùng để truy cập và thực hiện các thao tác với Cơ sở dữ liệu (Create, Read, Update, Delete).
10. **Singleton Pattern:** Một mẫu thiết kế đảm bảo một lớp chỉ có duy nhất một thực thể (instance) trong suốt quá trình chạy ứng dụng (ví dụ: `SocketClient`).

---

*Chúc các bạn phát triển dự án thành công! Nếu có thắc mắc, vui lòng liên hệ đội ngũ Lead.*





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
    * `model/`: Các thực thể dữ liệu POJO/DTOs (VD: `User.java`, `Product.java`, `Bid.java`).
    * `payload/`: (Request, Response, MessageType)
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
    * `controller/`: REST endpoints, xử lý request từ client
    * `service/`: Business Logic: ProductService, UserService, BidService
      ProductService, UserService, BidService (sử dụng repositories, làm việc với DTO từ common)
        * `AuctionService.java`: Logic kiểm tra giá, tính toán thời gian kết thúc.
    * `repository/` (**DAO**): Chứa code SQL để làm việc với Database thông qua Entity.
        * `entity/`: (JPA Entities: ProductEntity, UserEntity - có annotation DB) ProductRepository, UserRepository, ... (interface JpaRepository)
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
