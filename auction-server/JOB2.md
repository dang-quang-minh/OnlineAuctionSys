# Tuần 7 — Job 2: AuctionManager

Bản local viết theo tab [Chữ ký API đã chốt](https://docs.google.com/document/d/1QdSp3QnVSsMtZORZ4Wi-xohlH4ZUgrg09movlPOoHGs/edit?tab=t.h1k3bs95pbba).
Nhánh: `feat/week7-job2-auction-manager`, bắt đầu từ `origin/dev` tại `7bd7a6b`.
Sáu commit nháp ban đầu đã được push lên nhánh này. Bản sửa scheduler sau review được lưu bằng
các commit riêng theo file ở local, chưa push.
Lần fetch mới nhất thấy `origin/dev` đã lên `5abd9b3`, có API model mới của Job 1; nhánh Job 2 hiện
chưa tích hợp các commit upstream đó vào source chính.

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
- Nếu tới hạn phiên vẫn `OPEN`, scheduler đặt một lần kiểm tra tiếp theo sau 100 ms. Mỗi lần kiểm tra
  chỉ đặt thêm một lịch khi phiên vẫn `OPEN`; khi thành `RUNNING` thì đóng phiên, còn trạng thái kết thúc
  hoặc Manager đã dừng thì không tiếp tục kiểm tra.
- `close()` dừng scheduler và từ chối tạo phiên/đặt giá mới. Server phải gọi hàm này khi dừng.

Manager không tự `start()`, `markPaid()` hoặc `cancel()`. Nếu bên điều khiển vòng đời start phiên sau hạn,
lần kiểm tra tiếp theo sẽ phát hiện `RUNNING` và gọi `finish()`. Khoảng 100 ms là chu kỳ polling;
độ trễ thực tế còn phụ thuộc scheduler, hệ điều hành và khóa model, nên đây không phải bảo đảm đóng phiên
đúng tuyệt đối theo đồng hồ. Model Job 1 mới trên `origin/dev` đã kiểm tra `endsAt` trong private lock khi
nhận bid và từ chối bid quá hạn; kiểm tra đó bảo vệ thời điểm nhận bid độc lập với độ trễ polling.

## Sửa scheduler sau review

- Giữ việc theo dõi phiên `OPEN` sau hạn bằng chuỗi kiểm tra one-shot 100 ms, không bỏ mất lịch đóng
  khi phiên chuyển `RUNNING` muộn. Việc đặt lịch tiếp theo dùng `lifecycleLock` và kiểm tra lại `closed`
  để phối hợp với `close()`.
- Tính khoảng chờ sau khi đã lấy `lifecycleLock`, ngay trước `schedule()`, để không dùng lại delay cũ
  khi luồng tạo phiên phải chờ khóa. Nếu lúc đó deadline đã qua, khoảng chờ bằng 0.

Hai tình huống lỗi đã được tái hiện trong lần review và được bổ sung test hồi quy. Bản cũ thất bại 3/24 ca
khi thêm test mới; sau sửa, 25/25 ca qua, gồm ca dùng model Job 1 mới và scheduler thật để đóng phiên start trễ.
Việc tích hợp vẫn cần đưa API Job 1 mới và API thật của Job 3 vào nhánh trước khi xác nhận sẵn sàng merge.

Hai interface package-private cũ trong `server.service` được bỏ vì không có nơi sử dụng và thiết kế yêu cầu
chỉ có một bộ `Subject`/`Observer` do Job 3 cung cấp ở `common.observer`.
Không sửa file nào trong `auction-common` hoặc `auction-client`.

## Phụ thuộc tích hợp

| Bên cung cấp | API cần cho Job 2 | Trạng thái |
| --- | --- | --- |
| Job 1 | `Auction.getEndsAt(): Instant` | Có trên `origin/dev` tại `5abd9b3`, chưa tích hợp vào nhánh Job 2 |
| Job 1 | `Auction.placeBid(double, String): Bid` | Có trên upstream mới, kèm kiểm tra deadline trong lock |
| Job 1 | `Bid.getBidId(): String`, `Bid.getBidTime(): Instant` | Có trên upstream mới |
| Job 3 | `BidRequestDTO`, `BidDTO` đúng record đã chốt | Chưa có trên upstream được kiểm tra |
| Job 3 | `NetworkMessage(String, Serializable)`, `NEW_BID`, `getPayload()` | Chưa có đúng hợp đồng trên upstream |
| Job 3 | `common.observer.Subject` và `common.observer.Observer` | Chưa có trên upstream được kiểm tra |

Đây là các phụ thuộc cần tích hợp trước khi build toàn dự án có thể qua. Không dùng reflection,
không đọc bid cuối để suy ra bid vừa nhận, không tạo bản DTO/model thay thế trong mã production.

## Chạy kiểm tra

Sau khi tích hợp model Job 1 mới và API Job 3 vào nhánh, chạy từ thư mục gốc repo:

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
observer tự đăng ký/hủy, 16 request đồng thời, test hồi quy scheduler và các test dùng scheduler thật.
Một test dùng trực tiếp model Job 1 mới để xác nhận phiên start trễ được đóng và không nhận bid sau đó.
Mockito dùng startup agent
để không phụ thuộc việc JVM cho phép attach động.

Trong lần build trước, common/client qua nhưng server chưa compile do các phụ thuộc ở bảng trên.
Sau sửa scheduler chưa chạy lại build tích hợp vì source chính trên nhánh vẫn chưa có các API đó.
Test Manager được chạy riêng trong `target/job2-contract-check` (bị Git ignore): harness dùng source model thật
Job 1 lấy từ `origin/dev` tại `5abd9b3`, chỉ các DTO/message/observer của Job 3 còn là fixture theo hợp đồng.
Không thay model trong source chính. Kết quả riêng chưa chứng minh đã tích hợp toàn dự án; cần chạy lại
build/test sau khi đưa các thay đổi upstream và Job 3 vào nhánh.

Kết quả sau sửa: **25 test Manager qua, 0 failure/error**, compile bằng `--release 17` và thực thi trên
JDK 25 của máy; chưa chạy trên runtime JDK 17. Kết quả 21 test Manager và 13 test model đã nêu trước đây
là kết quả lịch sử trước bản sửa này.

Có thể chạy lại bộ kiểm thử riêng đang có trên máy từ gốc repo:

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -o -B -ntp -f auction-server/target/job2-contract-check/pom.xml '-Dtest=AuctionManagerTest' test
```

Thư mục fixture là bản sao của source tại lần kiểm tra, sẽ bị xóa khi chạy `clean`. Không dùng nó làm dependency
hoặc đưa lên Git. Báo cáo nằm ở `auction-server/target/job2-contract-check/target/surefire-reports/`.
Lệnh chọn riêng `AuctionManagerTest` để không chạy các probe review cũ trong thư mục bị ignore.
