# Hệ thống quản lý khách sạn Lucia (Lucia Hotel Management System)

Dự án này là một ứng dụng JavaFX được thiết kế để quản lý các hoạt động vận hành của khách sạn Lucia, bao gồm đặt phòng, quản lý khách hàng, nhân viên, dịch vụ và báo cáo doanh thu.

## 🚀 Tính năng chính

- **Quản lý đặt phòng (Booking):** Đặt phòng, nhận phòng, trả phòng và quản lý trạng thái phòng.
- **Quản lý khách hàng:** Lưu trữ thông tin khách hàng, lịch sử thuê phòng.
- **Quản lý nhân viên:** Quản lý hồ sơ nhân viên, phân quyền truy cập.
- **Quản lý dịch vụ & Tiện ích:** Danh mục dịch vụ, thêm dịch vụ vào hóa đơn.
- **Thống kê & Báo cáo:** Biểu đồ doanh thu, báo cáo mật độ sử dụng phòng (sử dụng JFreeChart).
- **Giao diện hiện đại:** Được thiết kế với thư viện **AtlantaFX** mang lại trải nghiệm chuyên nghiệp và bắt mắt.

---

## 🛠️ Yêu cầu hệ thống (Prerequisites)

Để chạy được dự án này trên máy của bạn, bạn cần cài đặt các thành phần sau:

1.  **Java JDK 17 hoặc 21 trở lên:** Khuyên dùng JDK 21.
2.  **JavaFX SDK 21.0.11 hoặc mới hơn:** Tải tại [Gluon](https://gluonhq.com/products/javafx/).
3.  **Microsoft SQL Server:** Phiên bản 2019 hoặc mới hơn.
4.  **IDE:** Visual Studio Code (VS Code) với gói **Extension Pack for Java**.

---

## 🏗️ Hướng dẫn cài đặt & Thiết lập

### 1. Thiết lập Cơ sở dữ liệu (SQL Server)
1.  Mở SQL Server Management Studio (SSMS).
2.  Mở và chạy file theo thứ tự trong thư mục `sql/`:
    - Chạy `Lucia_TaoBang.sql` để tạo database `LuciaHT` và cấu trúc các bảng.
    - Chạy `Lucia_DuLieu.sql` để nhập dữ liệu mẫu.
3.  Đảm bảo SQL Server đang chạy ở cổng mặc định `1433`.

### 2. Cấu hình đường dẫn trong dự án (Quan trọng)

Vì dự án sử dụng các thư viện ngoài và JavaFX SDK nằm ở các thư mục cố định trên máy cũ, bạn **PHẢI** cập nhật lại các đường dẫn sau để chạy được trên máy của mình.

#### A. Cập nhật `launch.json` (Để chạy và debug)
Mở tệp `.vscode/launch.json` và chỉnh sửa dòng `vmArgs`:

- Cập nhật đường dẫn đến thư mục `lib` của **JavaFX SDK** mà bạn đã tải về máy mình.
- Định dạng: `--module-path "ĐƯỜNG_DẪN_ĐẾN_JAVAFX_SDK/lib;${workspaceFolder}/lib"`

Ví dụ trên Windows:
```json
"vmArgs": "--module-path \"C:/java/javafx-sdk-21.0.1/lib;${workspaceFolder}/lib\" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web,atlantafx.base"
```

#### B. Cập nhật `.classpath` (Để IDE nhận diện thư viện)
Mở tệp `.classpath` ở thư mục gốc của dự án. Tìm và thay thế tất cả các đường dẫn tuyệt đối (bắt đầu bằng `G:/Code/...`) thành đường dẫn thực tế trên máy bạn.

**Các dòng cần chú ý:**
- Các entries của JavaFX (ví dụ: `javafx.controls.jar`).
- Entry của `atlantafx-base`.

Bạn có thể sửa thủ công hoặc vào VS Code: `Java Projects` panel -> `Referenced Libraries` -> Chuột phải và chọn `Add Library` để trỏ lại các file trong thư mục `lib` của dự án và `lib` của JavaFX SDK.

#### C. Cấu hình kết nối Database
Mở tệp `src/connectDatabase/ConnectDatabase.java`. Kiểm tra các thông số sau:
- `URL`: Địa chỉ server và tên database.
- `USER`: Tên đăng nhập SQL Server (mặc định là `sa`).
- `PASSWORD`: Mật khẩu của tài khoản SQL Server.

```java
private final String URL = "jdbc:sqlserver://localhost:1433;databaseName=LuciaHT;encrypt=true;trustServerCertificate=true;";
private final String USER = "sa";
private final String PASSWORD = "your_password_here";
```

---

## 🏃 Cách chạy ứng dụng

1.  Mở dự án bằng VS Code.
2.  Mở file `src/main/App.java`.
3.  Nhấn phím `F5` hoặc vào tab **Run and Debug** -> Chọn cấu hình **Lucia Hotel (Chạy App)** và nhấn nút Start.

---

## 📦 Thư viện sử dụng
Dự án có sử dụng các thư viện trong thư mục `lib/`:
- **AtlantaFX:** Themes giao diện hiện đại.
- **JFreeChart:** Vẽ biểu đồ thống kê.
- **Apache POI:** Hỗ trợ đọc/ghi file Excel.
- **JCalendar:** Thành phần chọn ngày tháng.
- **MSSQL JDBC Driver:** Kết nối cơ sở dữ liệu SQL Server.

Nếu có thắc mắc gì, vui lòng liên hệ tôi (RyezenKaito)!
