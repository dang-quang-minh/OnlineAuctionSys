package com.auction.app.common.model;

// 5 trạng thái của phiên đấu giá: đang mở, đang chạy, đã hoàn thành,
// đã được trả tiền, đã được hủy
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
