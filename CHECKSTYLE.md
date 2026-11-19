# Tuần 9 · Task 2 · Checkstyle

Checkstyle đọc file Java bằng UTF-8 và báo vi phạm ở mức `warning`. Đề xuất kiểm tra cả `src/main/java` và `src/test/java` trong ba module, bỏ qua mã sinh tự động và tài nguyên.

| Quy tắc | Áp dụng |
| --- | --- |
| Đặt tên Java | Kiểm tra tên package, kiểu, phương thức, tham số, biến cục bộ, field và hằng số. |
| Import | Dùng import tường minh, kể cả `static import`. Báo import thừa hoặc không sử dụng. |
| Dấu ngoặc và khoảng trắng | Kiểm tra dấu ngoặc khối và khoảng trắng quanh cú pháp, ví dụ sau dấu phẩy. |
| Độ dài dòng | Tối đa 120 ký tự. `LineLength` dùng ngoại lệ mặc định cho dòng `package` và `import`. |
| Thụt lề và dòng trống | Ưu tiên 4 dấu cách, cấm ký tự tab. Tạm tắt `Indentation` và `EmptyLineSeparator` vì các file cũ dùng nhiều cách trình bày. |

## Chạy và đọc kết quả

Từ thư mục gốc dự án, chạy goal Checkstyle trực tiếp theo cấu hình Maven hiện tại.

```powershell
.\mvnw.cmd -B checkstyle:check
```

Nếu Windows không đọc được đường dẫn có dấu khi gọi `mvnw.cmd`, gọi cùng Wrapper JAR từ PowerShell.

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -B checkstyle:check
```

Trên macOS/Linux, chạy `./mvnw -B checkstyle:check`. POM hiện tại chạy goal này trên mã chính. Phạm vi test được bật bằng cấu hình Maven ở phần bàn giao dưới đây.

Mỗi module ghi `target/checkstyle-result.xml`. Log Maven ghi đường dẫn, dòng, cột nếu có và tên quy tắc để mở đúng chỗ cần sửa.

## Vi phạm có sẵn trên `dev`

Đo trước và sau trên cùng commit `c8b14be` bằng Maven Checkstyle Plugin `3.6.0`, sử dụng engine `9.3` mặc định của plugin. Cả hai lần đo đều quét 48 file Java chính và 6 file Java test.

| Phạm vi | Cấu hình cũ | Cấu hình mới |
| --- | ---: | ---: |
| Mã chính | 57 | 4 |
| Mã test | 21 | 21 |
| Tổng | 78 | 25 |

25 cảnh báo hiện có gồm 19 vị trí thiếu khoảng trắng sau dấu phẩy trong `AuctionTest.java` và `BidTest.java`, 3 wildcard import trong `Auction.java`, `AuctionTest.java` và `BidTest.java`, 2 dòng quá 120 ký tự trong các exception, và 1 dấu `}` xuống dòng trước `finally` trong `Auction.java`. Cả 25 vị trí đều xuất hiện với cấu hình cũ. Khi review PR, đối chiếu file, quy tắc và đoạn code với báo cáo trên `dev` để phân biệt cảnh báo cũ với cảnh báo do thay đổi mới.

## Bàn giao Maven cho Task 1

Giữ plugin `maven-checkstyle-plugin` `3.6.0` hiện có. Trong cấu hình plugin của POM gốc, dùng đường dẫn từ root cho cả ba module, bật kiểm tra test và gắn goal `check` vào pha `verify`.

```xml
<configuration>
    <configLocation>${maven.multiModuleProjectDirectory}/checkstyle.xml</configLocation>
    <includeTestSourceDirectory>true</includeTestSourceDirectory>
    <excludeGeneratedSources>true</excludeGeneratedSources>
    <includeResources>false</includeResources>
    <includeTestResources>false</includeTestResources>
    <consoleOutput>true</consoleOutput>
    <violationSeverity>warning</violationSeverity>
    <failOnViolation>false</failOnViolation>
</configuration>
<executions>
    <execution>
        <id>checkstyle</id>
        <phase>verify</phase>
        <goals>
            <goal>check</goal>
        </goals>
    </execution>
</executions>
```

Chế độ báo cáo cho phép `clean verify` hiển thị cảnh báo sẵn có trong giai đoạn tích hợp. Nhóm thống nhất phạm vi và cách xử lý các cảnh báo cũ trước khi bật `failOnViolation` cho CI.

Lần kiểm tra local dùng JDK 25 theo POM hiện tại. Yêu cầu Java 17 của tuần 9 cần được Task 1 thống nhất với cấu hình JavaFX 25 đang có trước khi Task 3 chốt JDK trên CI.
