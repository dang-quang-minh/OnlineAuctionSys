# Tuần 8 — Job 2: xử lý lỗi trong luồng đấu giá

Nguồn yêu cầu: [Tuần 8](https://docs.google.com/document/d/1QdSp3QnVSsMtZORZ4Wi-xohlH4ZUgrg09movlPOoHGs/edit?tab=t.k16w6ywu28p7).
Thực hiện trong clone `OnlineAuctionSys-moi`, nhánh `feat/week8-job2-bid-error-handling`,
từ `dev` tại `c8b14be`. Đây là bảng hành vi bàn giao để Job 3 xác nhận và bổ sung test tổng thể.

## Thay đổi

- `Auction.placeBid()` từ chối bidderId rỗng/toàn khoảng trắng bằng `IllegalArgumentException`,
  kể cả khi gọi model trực tiếp. Null vẫn dùng `NullPointerException`.
- Tách giá âm/0/NaN/Infinity khỏi phép so sánh currentPrice, dùng constructor message có sẵn
  của `InvalidBidException`: "Giá đặt phải là số hữu hạn và lớn hơn 0."
- Không thay chữ ký public, DTO, errorCode hoặc các constructor exception của Job 1.
- Manager giữ việc gọi model đúng một lần, truyền nguyên exception và chỉ thông báo sau khi thành công.
  Bổ sung JavaDoc mô tả các nhóm lỗi; không thêm handler mạng hoặc sửa GUI của Job 4.

## Tình huống và hành vi mong đợi

Các dòng dưới giả định Manager chưa dừng. Khi Manager đã dừng, `processBid()` từ chối ngay
bằng `IllegalStateException` trước khi kiểm tra request.

| Tình huống | Exception / kết quả | errorCode | Dữ liệu và event |
| --- | --- | --- | --- |
| RUNNING, chưa hết hạn, giá hữu hạn > currentPrice | Trả `BidDTO` từ đúng Bid model tạo | Không có lỗi | Giá/history/winner cập nhật, một `NEW_BID` |
| Giá âm, 0 hoặc -0.0 | `InvalidBidException`, thông báo giá hữu hạn và > 0 | `INVALID_BID` | Giữ nguyên, không event |
| NaN, +Infinity, -Infinity | `InvalidBidException`, thông báo chung dễ hiểu, không in NaN/Infinity | `INVALID_BID` | Giữ nguyên, không event |
| Giá hữu hạn dương thấp hơn/bằng currentPrice | `InvalidBidException` với thông báo so sánh giá hiện có | `INVALID_BID` | Giữ nguyên, không event |
| OPEN / FINISHED / PAID / CANCELED | `AuctionClosedException` với trạng thái phiên | `AUCTION_CLOSED` | Giữ nguyên cả trạng thái, không event |
| RUNNING, thời điểm đặt giá >= endsAt | `AuctionClosedException` với trạng thái EXPIRED | `AUCTION_CLOSED` | Không tự finish trong placeBid; không đổi dữ liệu/event |
| Thời điểm đặt giá < endsAt dù chỉ 1 ns | Chấp nhận nếu các điều kiện khác hợp lệ | Không có lỗi | Một bid, một event |
| auctionId đối số null/rỗng/blank, không tồn tại hoặc khác request.auctionId | `IllegalArgumentException` | Không ép thành custom errorCode | Giữ nguyên, không event |
| request null, bidderId null | `NullPointerException` | Không ép thành custom errorCode | Giữ nguyên, không event |
| bidderId rỗng/blank | `IllegalArgumentException` | Không ép thành custom errorCode | Giữ nguyên, không event |
| Một observer ném runtime exception sau bid thành công | Log phía server, tiếp tục observer khác, vẫn trả DTO thành công | Không biến thành bid lỗi | Không rollback bid hay phát lại event |

### Thứ tự kiểm tra

Manager kiểm tra lifecycle, auctionId, request và ID khớp/tồn tại trước khi gọi model.
Model kiểm tra bidderId null/blank trước khi lấy khóa; trong khóa kiểm tra trạng thái,
deadline, tính hợp lệ của giá rồi mới ghi bid và cập nhật currentPrice.
Vì vậy, với bidderId hợp lệ, nếu vừa sai giá vừa đóng phiên thì ưu tiên `AuctionClosedException`.
Không tự trim ID hoặc sinh bidderId thay cho dữ liệu thiếu.

## Bảo toàn dữ liệu và hợp đồng

Giá, history, winner, trạng thái và danh sách event được chụp trước/sau các request bị từ chối.
Mọi kiểm tra nghiệp vụ và cập nhật bid vẫn dùng cùng private lock của Auction.
Không chuyển custom exception thành lỗi chung; Manager không bắt rồi nuốt lỗi từ `placeBid()`.
Không tự thêm luật seller không được bid, bước giá hoặc phí.

Hiện chưa có server socket handler chuyển lỗi thành `NetworkMessage.ERROR`.
Phần này dành cho tích hợp networking sau; giữ payload ERROR dạng String như hợp đồng,
không đưa exception, cause hoặc stack trace lên client. Log kỹ thuật phía server vẫn được phép giữ cause.

## Kiểm chứng

Thêm riêng `Week8BidErrorHandlingTest` đi cùng phần sửa Job 2, không sửa các file test hiện có
của Job 3. Có 26 ca parameterized/regression cho giá, ID, trạng thái và biên deadline.
Test deadline kiểm soát `Instant.now()` trên luồng gọi bằng Mockito static mock có đóng scope,
không sleep và không thay API hoặc logic clock của production.

Đã chạy từ root repo trên JDK 25:

```powershell
java '-Dmaven.multiModuleProjectDirectory=.' -classpath .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -o -B -ntp clean verify
```

Kết quả: **BUILD SUCCESS — 73 test qua** (16 common, 3 client, 54 server),
0 failure/error/skipped. Trước sửa, bộ mới có 7 failure/26 ca: 4 ID rỗng/blank và 3 message không hữu hạn.
Sau sửa, toàn bộ 26 ca mới qua; các test tuần 7 và phần Job 1/4 tuần 8 vẫn qua.
Source dùng model/DTO/exception thật, không dùng API fixture. Các thay đổi được lưu riêng trên nhánh Job 2.
