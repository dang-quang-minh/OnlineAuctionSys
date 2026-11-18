package com.auction.app.client.demoData;

import com.auction.app.client.demoModel.AuctionViewData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Temporary Week 8 demo data source for the JavaFX list.
 *
 * <p>These auction rows are presentation fixtures; they are not received from
 * or sent to the server. Socket integration belongs to Week 9.</p>
 */
public final class DemoAuctionData {
    private DemoAuctionData() {
    }

    public static List<AuctionViewData> load() {
        Instant now = Instant.now();
        return List.of(
                new AuctionViewData(
                        "AUC-001",
                        "Tranh sơn dầu phong cảnh",
                        "nguyen.minh.anh",
                        "800.000 đ",
                        "1.200.000 đ",
                        12,
                        now.plus(8, ChronoUnit.HOURS),
                        "ĐANG DIỄN RA",
                        "Tác giả: Nguyễn Hoàng; tranh 60 x 80 cm, kèm chứng nhận nguồn gốc."),
                new AuctionViewData(
                        "AUC-002",
                        "Xe máy cổ Honda Cub 50",
                        "tran.hoang.nam",
                        "15.000.000 đ",
                        "18.500.000 đ",
                        8,
                        now.plus(10, ChronoUnit.HOURS),
                        "ĐANG DIỄN RA",
                        "Năm sản xuất: 1982; máy nguyên bản, đã kiểm tra giấy tờ."),
                new AuctionViewData(
                        "AUC-003",
                        "Bình gốm nghệ thuật Bát Tràng",
                        "le.thu.ha",
                        "500.000 đ",
                        "850.000 đ",
                        17,
                        now.plus(2, ChronoUnit.HOURS),
                        "ĐANG DIỄN RA",
                        "Nguồn gốc: Bát Tràng, Việt Nam; sản phẩm thủ công men rạn."));
    }
}
