# Module: Common (Shared Library)

Module `common` đóng vai trò là "Từ điển chung" của toàn bộ hệ thống. Bất kỳ sự thay đổi nào về cấu trúc dữ liệu ở đây sẽ tự động được áp dụng cho cả Client và Server, đảm bảo tính nhất quán cao nhất.

Mọi dữ liệu truyền qua mạng đều phải được đóng gói bằng các Object trong module này trước khi chuyển sang định dạng JSON.

## 📂 Cấu trúc logic nghiệp vụ (Folder Structure Logic)

```text
common/src/main/java/org/example/common/
├── model/           # Chứa các POJO/DTOs, định nghĩa các thực thể dữ liệu kinh doanh (User, Product, Bid...)
├── payload/         # Định nghĩa giao thức mạng, bao gồm các cấu trúc gói tin (Request, Response) và các lệnh MessageType để Client và Server hiểu nhau.
└── util/            # Chứa các công cụ hỗ trợ tiện ích dùng chung (ví dụ: công cụ chuyển đổi Java Object <-> JSON).
```

## 🛠 Mục đích các thành phần

### 1. `model/` (Tầng dữ liệu lõi)
Chứa các class biểu diễn dữ liệu thuần túy. 
- Nhiệm vụ: Đóng gói dữ liệu để Server tương tác với Database và Client hiển thị lên màn hình.
- Nguyên tắc: Không chứa các logic nghiệp vụ phức tạp, chỉ tập trung vào cấu trúc dữ liệu và quan hệ kế thừa (OOP).

### 2. `payload/` (Tầng giao thức)
Chuẩn hóa giao thức giao tiếp qua mạng Socket. Thay vì Client và Server truyền chuỗi String tự do, tất cả phải tuân theo cấu trúc:
- **MessageType**: Liệt kê các hành động cụ thể (Ví dụ: Yêu cầu đăng nhập, Đặt giá thầu, Thông báo lỗi).
- **Request / Response**: Đóng gói MessageType cùng với phần dữ liệu thực tế (dạng JSON) để tạo thành một gói tin hoàn chỉnh và an toàn khi truyền tải.

### 3. `util/` (Tầng công cụ)
Trung tâm cung cấp các hàm hỗ trợ chung (static utility methods). Điển hình là việc chuyển đổi qua lại giữa cấu trúc Java Object phức tạp và định dạng chuỗi JSON dễ dàng truyền tải qua Socket.
