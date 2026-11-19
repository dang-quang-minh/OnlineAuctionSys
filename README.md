# OnlineAuctionSys
btl CR7 CS2043

## Build bằng Maven

Từ thư mục gốc của dự án, chạy:

```bash
.\mvnw.cmd -B clean verify
```

Các module `auction-common`, `auction-client` và `auction-server` được build cùng
với POM tổng ở thư mục gốc. Trên Linux/macOS, dùng `./mvnw -B clean verify`.

## Chạy demo trên Windows

Dùng JDK 25. Trong PowerShell tại thư mục gốc dự án:

```powershell
.\run-demo.ps1
```

Lần đầu script build client/common và tải các dependency cần thiết. Cửa sổ JavaFX mở với
tài khoản demo `demo` / `demo`. Sau khi đã chạy thành công, có thể mở nhanh bằng
`.\run-demo.ps1 -SkipBuild`; chạy lại không có `-SkipBuild` sau khi sửa hoặc pull code.

Nếu PowerShell chặn script chưa ký, dùng lệnh sau để cho phép riêng lần chạy này,
không thay đổi execution policy của máy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-demo.ps1
```

Runner gọi Java trực tiếp với classpath ứng dụng tương đối, tránh lỗi
`ClassNotFoundException: com.auction.app.client.Launcher` khi `javafx:run` của plugin 0.0.8
truyền đường dẫn chứa tiếng Việt qua Java con trên Windows dùng native encoding Cp1252.
Script dừng nếu build lỗi, không tiếp tục mở bản build cũ.

## Chạy Server

Server và Client chạy độc lập. Từ thư mục gốc, build toàn bộ project trước:

```powershell
.\mvnw.cmd -B clean install -DskipTests
```

Khởi động Server trên cổng mặc định `5050`:

```powershell
java -cp "auction-server\target\classes;auction-common\target\classes" `
    com.auction.app.server.ServerLauncher 5050
```

Server tạo một phiên mẫu trong `AuctionManager` để Client có dữ liệu hiển thị.

## Chạy Client

Giữ Server đang chạy, sau đó mở một PowerShell khác:

```powershell
.\run-demo.ps1
```

Có thể chạy nhiều Client trên các máy khác nhau. Mặc định Client kết nối
`localhost:5050`; khi Server ở máy khác, cấu hình host và port trước khi chạy:

```powershell
$env:JAVA_TOOL_OPTIONS='-Dauction.server.host=192.168.1.100 -Dauction.server.port=5050'
.\run-demo.ps1 -SkipBuild
```

Đăng nhập demo bằng `demo` / `demo`. Client tải danh sách phiên và cập nhật
dữ liệu qua Socket; đăng ký tài khoản chưa được nối vào Server.
