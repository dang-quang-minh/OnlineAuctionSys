# Tuần 7 — Job 2: AuctionManager

Bản local viết theo tab [Chữ ký API đã chốt](https://docs.google.com/document/d/1QdSp3QnVSsMtZORZ4Wi-xohlH4ZUgrg09movlPOoHGs/edit?tab=t.h1k3bs95pbba).
Nhánh: `feat/week7-job2-auction-manager`, bắt đầu từ `origin/dev` tại `7bd7a6b`.
Bản nháp Job 2 được lưu bằng các commit riêng theo file trên nhánh local. Chưa push.

## Luồng xử lý

- `createAuction(Auction)` trả lại phiên đã đăng ký, từ chối ID trùng và đặt lịch theo `getEndsAt()`.
- `getAuction(String)` trả `Optional<Auction>`; `getAllAuctions()` trả bản sao của map.
- `getActiveAuctions()` trả snapshot không sửa được, chỉ có `OPEN` và `RUNNING`. Các giá trị vẫn là model gốc.
- `processBid(String, BidRequestDTO)` kiểm tra ID, gọi `Auction.placeBid()` đúng một lần,
  chuyển chính `Bid` vừa nhận thành `BidDTO`, phát một `NEW_BID`, rồi trả DTO đó.
- Bid lỗi giữ nguyên exception của model và không phát thông báo. Phiên không tồn tại hoặc ID không khớp
  ném `IllegalArgumentException`.
- Observer dùng `CopyOnWriteArrayList`, không đăng ký trùng cùng observer. Một observer ném runtime exception
  sẽ được ghi log; các observer còn lại vẫn nhận event và bid đã thành công vẫn trả về bình thường.
- Scheduler chỉ gọi `finish()` cho phiên `RUNNING`. `finish()` và `placeBid()` phải dùng cùng private lock
  trong model của Job 1; Manager không tự khóa hoặc sửa giá/lịch sử bid.
- `close()` dừng scheduler và từ chối tạo phiên/đặt giá mới. Server phải gọi hàm này khi dừng.

Manager không tự `start()`, `markPaid()` hoặc `cancel()`. Với thiết kế hiện tại, bên điều khiển vòng đời
phải start phiên trước `endsAt`. Nếu phiên vẫn `OPEN` lúc hết hạn, lịch đóng sẽ bỏ qua để không gọi chuyển
trạng thái sai; không được start lại phiên đó sau hạn. Luồng tự bắt đầu phiên/khôi phục phiên quá hạn chưa
được định nghĩa trong API tuần 7.

## Các lỗi đã xác nhận khi review

- Nếu callback hết hạn thấy phiên còn `OPEN`, nó kết thúc và không theo dõi nữa. Nếu sau đó phiên
  chuyển `RUNNING`, phiên không còn lịch tự đóng. Điều kiện start trước hạn ở trên hiện chưa được API thực thi.
- `delayNanos` đang được tính trước khi lấy `lifecycleLock`. Khi nhiều luồng tạo phiên phải chờ khóa,
  lịch được đăng ký với khoảng chờ cũ, khiến phiên đóng muộn. Cần tính lại delay ngay trước `schedule()`.

Hai tình huống đã được tái hiện bằng API fixture local trong lần review. Source chính chưa sửa hai lỗi này;
các commit hiện tại là bản nháp để xem xét, chưa sẵn sàng merge.

Hai interface package-private cũ trong `server.service` được bỏ vì không có nơi sử dụng và thiết kế yêu cầu
chỉ có một bộ `Subject`/`Observer` do Job 3 cung cấp ở `common.observer`.
Không sửa file nào trong `auction-common` hoặc `auction-client`.

## Phụ thuộc còn thiếu trên dev

| Bên cung cấp | API cần cho Job 2 |
| --- | --- |
| Job 1 | `Auction.getEndsAt(): Instant` |
| Job 1 | `Auction.placeBid(double, String): Bid` |
| Job 1 | `Bid.getBidId(): String`, `Bid.getBidTime(): Instant` |
| Job 3 | `BidRequestDTO`, `BidDTO` đúng record đã chốt |
| Job 3 | `NetworkMessage(String, Serializable)`, `NEW_BID`, `getPayload()` |
| Job 3 | `common.observer.Subject` và `common.observer.Observer` |

Đây là các phụ thuộc cần tích hợp trước khi build toàn dự án có thể qua. Không dùng reflection,
không đọc bid cuối để suy ra bid vừa nhận, không tạo bản DTO/model thay thế trong mã production.

## Chạy kiểm tra

Sau khi Job 1/3 cập nhật API, chạy từ thư mục gốc repo:

```powershell
.\mvnw.cmd -pl auction-server -am test
.\mvnw.cmd clean verify
```

Nếu wrapper `.cmd` không tìm được lớp main do đường dẫn chứa dấu, có thể gọi cùng wrapper JAR qua PowerShell:

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -B -ntp clean verify
```

Repo hiện cấu hình Java 25, khác với Java 17 trong tài liệu. Job 2 chỉ dùng API/ngôn ngữ Java 17;
không thay cấu hình Java chung của nhóm.

`AuctionManagerTest` kiểm tra orchestration bằng JUnit/Mockito, gồm lịch đóng, exception, event,
observer tự đăng ký/hủy, 16 request đồng thời và một test dùng scheduler thật. Mockito dùng startup agent
để không phụ thuộc việc JVM cho phép attach động.

Trong lần kiểm tra local, build toàn repo qua common/client nhưng server chưa compile do các phụ thuộc
ở bảng trên. Test Manager được chạy riêng với API fixture trong `target/job2-contract-check` (bị Git ignore).
Fixture chỉ khai báo kiểu/hợp đồng để Mockito hoạt động, không phải model thật hay mã bàn giao của Job 1/3.
Kết quả test riêng không chứng minh đã tích hợp thành công; cần chạy lại build/test với model thật sau khi merge.

Kết quả đã chạy: **21 test Manager qua, 0 failure/error**. Lần chạy riêng compile bằng `--release 17`
và thực thi trên JDK 25 của máy; chưa chạy trên runtime JDK 17. 13 test model đang có của Job 1 cũng qua
trong lần build toàn repo, nhưng chúng chưa kiểm tra đầy đủ hợp đồng mới.

Có thể chạy lại bộ kiểm thử riêng đang có trên máy từ gốc repo:

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -o -B -ntp -f auction-server/target/job2-contract-check/pom.xml test
```

Thư mục fixture là bản sao của source tại lần kiểm tra, sẽ bị xóa khi chạy `clean`. Không dùng nó làm dependency
hoặc đưa lên Git. Báo cáo nằm ở `auction-server/target/job2-contract-check/target/surefire-reports/`.
