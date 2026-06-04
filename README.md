# 🏛️ Bidding Online System - Hệ Thống Đấu Giá Trực Tuyến

<div align="center">

![Java](https://img.shields.io/badge/Java-21--LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-FF0000?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-9.5.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A22?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)

</div>

## 📖 Overview (Tổng quan dự án)

Dự án Hệ thống đấu giá trực tuyến (Bidding System) được thiết kế và xây dựng theo kiến trúc **Client-Server** nguyên bản, vận hành thông qua kết nối mạng **Socket (TCP/IP)** và định dạng dữ liệu **JSON**. Dự án sử dụng ngôn ngữ **Java 21 LTS** kết hợp với **JavaFX 21.0.6** cho giao diện đồ họa. Phía Server được phát triển bằng **Java Core** xử lý đa luồng (Multi-threading) và tương tác với cơ sở dữ liệu **MySQL** thông qua JDBC và HikariCP.

Hệ thống nhằm mang lại một nền tảng đấu giá thời gian thực (real-time) minh bạch, an toàn và công bằng. Các tính năng nổi bật bao gồm chống bắn tỉa (Anti-sniping), đấu giá tự động (Auto-bidding) và xử lý tương tranh (Concurrency Bidding) hiệu quả.

---

## 📚 Detailed Documentation (Tài liệu chi tiết)

Để tìm hiểu chi tiết về thiết kế kiến trúc, sơ đồ cơ sở dữ liệu (ERD), sơ đồ luồng (Sequence Diagrams) và các Use Case, vui lòng truy cập trung tâm tài liệu:

👉 **[Bidding System Documentation Center](./docs/README.md)**

Các tài liệu quan trọng khác:
- 📄 **[Báo cáo Bài tập lớn](./docs/BaoCaoHeThongDauGia_Nhom16.pdf)**
- 🎥 **[Video Demo Hệ Thống (Drive)](https://drive.google.com/drive/folders/1sZlB2uVs7Br4eJ3b-fTCqjWCtNf3f3Pp?usp=sharing)**

---

## 🏗️ Multi-Module Architecture (Cấu trúc hệ thống)

Dự án áp dụng mô hình **Maven Multi-Module** nhằm tối ưu hóa tổ chức mã nguồn, chia tách các mối quan tâm (Separation of Concerns) và chia sẻ code chung dễ dàng.

### 1. 📦 [`common`](./common/README.md) - Shared Library
Đóng vai trò là "ngôn ngữ chung" cho hệ thống, chứa các POJO, DTO, các hằng số và cấu trúc gói tin giao tiếp (Payloads) chung giữa Client và Server. Mọi thay đổi về cấu trúc mạng đều tập trung ở đây.

### 2. 🖥️ [`server`](./server/README.md) - Backend System
Xử lý toàn bộ logic nghiệp vụ cốt lõi, thao tác với Database và quản lý kết nối Socket của các Client. Đảm bảo tính toán độc lập, an toàn dữ liệu trong môi trường đa luồng.

### 3. 🎨 [`client`](./client/README.md) - Frontend GUI
Giao diện người dùng đồ họa (GUI) xây dựng bằng JavaFX theo mô hình MVC, cung cấp trải nghiệm mượt mà và nhận thông báo theo thời gian thực từ hệ thống Server mà không bị treo giao diện.

---

## 🚀 Tech Stack (Công nghệ sử dụng)

- **Ngôn ngữ:** Java 21 LTS (LTS).
- **Giao diện (Frontend):** JavaFX 21.0.6, ControlsFX, BootstrapFX.
- **Máy chủ (Backend):** Java Core Socket (Multi-threading), JDBC, HikariCP Connection Pool, BCrypt (Bảo mật mật khẩu).
- **Cơ sở dữ liệu:** MySQL 9.5.0 (Cấu hình qua Docker Compose).
- **Quản lý build & thư viện:** Maven 3.13.0.
- **Định dạng dữ liệu mạng:** GSON 2.11.0 (Xử lý Object <-> JSON).

---

## 📡 Communication Protocol (Giao thức truyền thông)

Toàn bộ dữ liệu qua lại giữa Client và Server được trừu tượng hóa và đóng gói vào các đối tượng (Objects) ở module `common`, sau đó chuyển thành định dạng JSON.

**Mô hình giao tiếp tiêu chuẩn:**
- Client gửi một đối tượng `Request` (có `MessageType`, `Payload` cụ thể, `Token`).
- Server xử lý và trả về đối tượng `Response` (có trạng thái thành công/thất bại, dữ liệu trả về, thông báo lỗi nếu có).
- Server chủ động phát sóng (Broadcast) thông tin cho tất cả các Client thông qua kết nối Socket ngầm để cập nhật giao diện (Ví dụ: thông báo có người trả giá cao hơn).

---

## 🛠️ Setup & Run Instructions (Hướng dẫn chạy dự án)

### 1. Yêu cầu hệ thống:
- **JDK 21 LTS** đã được cài đặt và cấu hình biến môi trường (`JAVA_HOME`).
- **Maven 3.9+**.
- **Docker** và **Docker Compose** (Dành cho việc khởi tạo nhanh Database).

### 2. Cấu hình môi trường (.env):
Hệ thống sử dụng file `.env` để quản lý các biến môi trường (như tài khoản Cloudinary, thông tin DB).
- Tại thư mục gốc, sao chép file `.env.example` thành `.env`:
```bash
cp .env.example .env
```
- Mở file `.env` và điền các thông tin cần thiết (đặc biệt là Cloudinary để upload ảnh sản phẩm).

### 3. Khởi chạy Database:
Đảm bảo cổng `3306` đang rảnh. Mở Terminal tại thư mục gốc của dự án và chạy:
```bash
docker-compose up -d
```
Quá trình này sẽ tải MySQL, tạo database `bidding_db` và chạy các kịch bản `.sql` (nếu được mount) để tạo bảng dữ liệu mẫu.

### 4. Biên dịch và tải thư viện:
Biên dịch toàn bộ hệ thống để đảm bảo `common` được cài đặt cho `client` và `server`:
```bash
mvn clean install
```

### 5. Khởi chạy Server:
Mở một Terminal mới tại thư mục dự án:
```bash
mvn -pl server exec:java -Dexec.mainClass="org.example.server.ServerApp"
```

### 6. Khởi chạy Client:
Mở một Terminal mới (có thể mở nhiều Terminal nếu muốn test với nhiều người dùng) và khởi chạy ứng dụng JavaFX:
```bash
mvn -pl client javafx:run
```

---

## 📖 Glossary (Khái niệm cốt lõi)

- **Socket / TCP-IP:** Kênh giao tiếp hai chiều thời gian thực giữa 1 Client và 1 luồng xử lý trên Server.
- **JSON Serialization:** Quá trình chuyển đổi từ cấu trúc Class Java sang chuỗi ký tự JSON và ngược lại qua thư viện `GSON`.
- **Luồng (Thread):** Mỗi một Client kết nối đến, Server tạo một Thread độc lập (`ClientHandler`) để lắng nghe.
- **JavaFX Platform.runLater():** Cơ chế bắt buộc trên Client để cập nhật giao diện từ luồng mạng ngầm sang luồng UI chính.
- **Concurrency (Tương tranh):** Xử lý khi có >= 2 Client đặt giá trong cùng một thời điểm, đảm bảo chỉ 1 người thành công và giá trị không bị ghi đè sai lệnh.
- **Anti-sniping:** Thuật toán chống hành vi chờ 1-2 giây cuối mới đặt giá; nếu có đặt giá ở những giây cuối, Server tự động cộng thêm thời gian cho phiên đấu giá.
