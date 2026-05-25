# 📦 Module: Common (Shared Library)

Module `common` là xương sống về mặt cấu trúc dữ liệu của toàn bộ dự án. Nó được định nghĩa là một thư viện dùng chung cho cả `client` và `server`. Mục tiêu của module này là tái sử dụng mã nguồn và đảm bảo giao thức liên lạc (Network Protocol) cũng như cấu trúc đối tượng (Data Models) nhất quán giữa hai đầu của kết nối mạng.

> ⚠️ **Quy tắc vàng:** Không bao giờ viết logic nghiệp vụ (Service logic, Database CRUD, hay UI Render) trong module này. Mọi thứ trong `common` phải là dữ liệu thuần túy (POJO) hoặc các tiện ích dùng chung (Utilities).

---

## 📂 Architecture Logic (Cấu trúc thư mục)

```text
common/src/main/java/org/example/common/
├── dto/                 # Chứa các gói tin dữ liệu Request/Response/Notify giữa Client và Server.
│   ├── notify/          # Thông báo từ Server chủ động đẩy xuống Client (VD: BidUpdateNotify).
│   ├── request/         # Gói tin Client yêu cầu Server xử lý (VD: LoginRequest, BidRequest).
│   └── response/        # Gói tin Server phản hồi lại Client (VD: ProductResponse).
├── model/               # Các Model biểu diễn thực thể của nghiệp vụ, ánh xạ tương đồng với Database.
│   ├── enums/           # Các Enum dùng chung (AuctionStatus, MessageType, UserRole).
│   ├── product/         # Thực thể Sản phẩm (Product, Electronics, Art...) dùng kế thừa (Inheritance).
│   ├── user/            # Thực thể Người dùng (User, Admin, Member).
│   └── (Auction, Bid, AutoBid, Transaction...)
├── payload/             # Wrapper classes chuẩn hóa cấu trúc đóng gói gói tin mạng.
│   ├── ErrorDetail.java
│   ├── Request.java     # Cấu trúc chung của 1 Request (Chứa MessageType, Token và DTO tương ứng).
│   └── Response.java    # Cấu trúc chung của 1 Response (Chứa Success Flag, Data DTO và ErrorDetail).
└── util/                # Công cụ tiện ích (Utilities).
    ├── Config.java      # Cấu hình tĩnh (VD: cổng kết nối mặc định).
    ├── FileLogger.java  # Hỗ trợ ghi log đơn giản.
    └── JsonConverter.java # Tiện ích chuyển đổi tĩnh giữa Object và JSON sử dụng GSON.
```

---

## 🛠 Nguyên tắc phát triển (Development Guidelines)

### 1. Quản lý thực thể (Data Models)
Tất cả các lớp ở đây đều sử dụng làm đối tượng chứa dữ liệu để hệ thống Server thao tác lưu xuống database, và hệ thống Client hiển thị lên giao diện.
- Nên thiết lập các trường là `private`.
- Có đầy đủ các `Constructors`, `Getters` và `Setters`.
- Không phụ thuộc vào thư viện liên quan đến JavaFX (của Client) hay JDBC (của Server).

### 2. Quản lý luồng gói tin (Payloads & DTOs)
Mạng Socket truyền dữ liệu thô. Để dễ quản lý, toàn bộ hệ thống sử dụng định dạng JSON, đóng gói bên trong đối tượng `Request` hoặc `Response`.
- Khi bạn muốn thêm một chức năng mạng mới (ví dụ: `XoaSanPham`), bạn phải:
  1. Thêm Enum `DELETE_PRODUCT` vào lớp `MessageType`.
  2. Tạo một DTO `ProductDeleteRequest` trong package `dto.request` để chứa `productId` cần xoá.
  3. Client đóng gói `ProductDeleteRequest` vào class `Request` và truyền chuỗi JSON qua Socket.

### 3. Serialization (Chuyển đổi dữ liệu)
Chúng ta dùng `GSON` (phiên bản `2.11.0`) cho việc chuyển đối chuỗi JSON sang đối tượng (Deserialize) và ngược lại (Serialize). Lớp `JsonConverter.java` đóng vai trò trung tâm hỗ trợ việc này, che giấu sự phức tạp khi xử lý các đối tượng đa hình (Polymorphism) như class `Product` (có thể là `Art`, `Electronics`...).

---

## ⚙️ Hướng dẫn build module
Khi bạn thực hiện thay đổi tại module `common`, bạn phải biên dịch lại bằng Maven để module `client` và `server` nhận được phiên bản mới nhất.

```bash
cd common
mvn clean install
```
Hoặc tại thư mục gốc của toàn dự án:
```bash
mvn clean install
```
