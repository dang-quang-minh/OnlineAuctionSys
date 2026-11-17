# Tuần 7 — Job 2: AuctionManager

Nhánh làm việc: `feat/week7-job2-auction-manager`.
Đã tích hợp `origin/dev` tại `d0f0777` vào nhánh Job 2 bằng merge `5934fd5`, gồm model Job 1 và hợp đồng Job 3.
Đã cập nhật tiếp `origin/dev` tại `f0f7bbf` (PR #29, Job 4) bằng merge `22dcde1`, giữ nguyên Manager Job 2.
API theo tab [Chữ ký API đã chốt](https://docs.google.com/document/d/1QdSp3QnVSsMtZORZ4Wi-xohlH4ZUgrg09movlPOoHGs/edit?tab=t.h1k3bs95pbba).

## Luồng nghiệp vụ

- `createAuction(Auction)` trả lại phiên đã đăng ký, từ chối ID trùng và đặt lịch theo `getEndsAt()`.
- `getAuction(String)` trả `Optional<Auction>`; `getAllAuctions()` trả bản sao của map.
- `getActiveAuctions()` trả snapshot không sửa được, chỉ gồm `OPEN` và `RUNNING`. Giá trị vẫn là model gốc.
- `processBid(String, BidRequestDTO)` kiểm tra ID, gọi `Auction.placeBid()` đúng một lần,
  chuyển chính `Bid` vừa được nhận thành `BidDTO`, phát một `NEW_BID`, rồi trả DTO đó.
- Bid lỗi giữ nguyên exception của model, không phát event. Phiên không tồn tại hoặc ID không khớp
  ném `IllegalArgumentException`.
- Observer dùng `CopyOnWriteArrayList`, tránh đăng ký trùng. Một observer ném runtime exception
  được ghi log; các observer còn lại vẫn nhận event và bid thành công vẫn được trả về.
- Manager không giữ giá hoặc lịch sử bid riêng. Việc kiểm tra trạng thái, giá, deadline và cập nhật
  dữ liệu cùng private lock thuộc model Job 1.
- `close()` dừng scheduler và từ chối tạo phiên/đặt giá mới. Server gọi hàm này khi dừng.

## Scheduler

Manager không tự `start()`, `markPaid()` hoặc `cancel()`.

Lịch đóng đầu tiên được đặt theo `endsAt`. Khoảng chờ được tính sau khi đã lấy `lifecycleLock`,
ngay trước `schedule()`; nếu deadline đã qua, khoảng chờ bằng 0.
Nếu tới hạn phiên vẫn `OPEN`, scheduler giữ một chuỗi kiểm tra one-shot cách nhau 100 ms.
Khi phiên thành `RUNNING`, lần kiểm tra tiếp theo gọi `finish()`. Khi thấy trạng thái kết thúc
hoặc Manager đã dừng, chuỗi kiểm tra dừng. Việc đăng ký kiểm tra tiếp theo dùng cùng khóa với `close()`.

100 ms là khoảng kiểm tra, không phải cam kết độ trễ tuyệt đối: scheduler, hệ điều hành và khóa model
có thể làm callback chạy muộn. Model thật đã kiểm tra `endsAt` bên trong private lock khi nhận bid,
nên bid quá hạn bị từ chối độc lập với thời điểm callback đóng phiên.

## Tích hợp Job 1/3

| Phần | Trạng thái trên nhánh Job 2 |
| --- | --- |
| `Auction`, `Bid`, `AuctionStatus` | Model thật đã tích hợp từ `dev` |
| `BidRequestDTO`, `BidDTO`, `AuctionDTO`, `LoginDTO`, `LoginResultDTO` | Record Serializable thật của Job 3 |
| `NetworkMessage` | Dùng constructor `(String, Serializable)`, `getPayload()` và các hằng message thật |
| `common.observer.Subject` / `Observer` | Giữ interface thật của Job 3, bỏ bộ package-private cũ ở server |

Không còn dùng API fixture để build/test Job 2. Những thay đổi trong `auction-common` đến từ merge
công việc Job 1/3; Job 2 không tạo lại DTO, protocol hoặc luật model.

## Tương thích demo Job 4

Demo từ `origin/feat/week-7-client`, commit `8c64d73`, đã merge vào `dev` qua PR #29 tại `f0f7bbf`
và đã tích hợp vào nhánh Job 2.
Đăng nhập demo dùng `demo/demo`, danh sách lấy từ `DemoAuctionData` và dùng DTO của Job 3.
`MainPageController` nhận `AUCTION_LIST` với payload `ArrayList<AuctionDTO>`.
Chưa có luồng socket nối UI với Manager và chưa xử lý `NEW_BID` trên màn danh sách.

Khi nối networking, lớp biên server chuyển phiên từ Manager thành `ArrayList<AuctionDTO>` rồi bọc
`NetworkMessage.AUCTION_LIST`. Không truyền trực tiếp map/model qua mạng và không cần thêm API Manager
chỉ để phục vụ mock UI.

Phần Job 4 sửa `AuctionManager.java` bằng bản cũ chỉ lọc `OPEN` đã được xử lý khi merge:
giữ nguyên Manager Job 2, bao gồm cả `OPEN`/`RUNNING`, luồng bid và scheduler.

## Chạy kiểm tra

Từ gốc repo, dùng JDK 25 theo cấu hình Maven hiện tại:

```powershell
.\mvnw.cmd clean verify
```

Nếu wrapper `.cmd` không tìm được lớp main do đường dẫn chứa dấu, gọi cùng wrapper JAR từ PowerShell:

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -B -ntp clean verify
```

`AuctionManagerTest` kiểm tra orchestration, observer, 16 request đồng thời, các lỗi scheduler đã sửa
và đóng phiên bắt đầu trễ bằng model thật với scheduler thật.
`AuctionManagerIntegrationTest` kiểm tra bid/event/serialize với model và DTO thật,
request bị từ chối không tạo event, và nhiều bid đồng thời không mất dữ liệu.
Mockito dùng startup agent cho các unit test; không dùng class model/DTO thay thế.

Báo cáo Maven nằm trong `auction-common/target/surefire-reports/` và
`auction-server/target/surefire-reports/`. Code UI Job 4 đến từ merge upstream;
Manager cũ trong nhánh demo không ghi đè Manager Job 2.

## Kết quả xác minh ngày 17/11/2026

- `clean verify` toàn repo sau khi tích hợp cả PR #29 của Job 4: **BUILD SUCCESS**,
  41 test qua (13 common, 28 server), không failure/error/skipped.
  Build và test dùng JDK 25, model/DTO/observer thật trong repo; không còn API fixture.
- `-pl auction-client -am clean verify` với 11 file client từ demo `8c64d73` trong worktree riêng:
  **BUILD SUCCESS**, 13 test common qua, 13 source client compile/package được. Manager Job 2 được giữ nguyên.
- Các test Job 2 gồm 25 test orchestration/scheduler và 3 test tích hợp model, event, serialize và concurrency.
- Chưa chạy GUI để kiểm tra hình ảnh/tương tác trực tiếp; chưa nối socket giữa demo và Manager.
