# OnlineAuctionSys
btl CR7 CS2043

## Build bằng Maven

Từ thư mục gốc của dự án, chạy:

```bash
mvnw.cmd clean verify
```

Các module `auction-common`, `auction-client` và `auction-server` được build cùng
với POM tổng ở thư mục gốc. Trên Linux/macOS, dùng `./mvnw clean verify`.

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

Demo hiện có đăng nhập, xem chi tiết phiên, làm mới, đăng xuất và đăng ký mô phỏng.
Dữ liệu vẫn là mock; đăng ký chưa lưu tài khoản và giao diện chưa nối luồng đặt giá với server.
