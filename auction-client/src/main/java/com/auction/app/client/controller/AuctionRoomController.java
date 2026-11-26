package com.auction.app.client.controller;

import com.auction.app.client.demoModel.AuctionViewData;
import com.auction.app.client.network.ClientSession;
import com.auction.app.client.view.ViewNavigator;
import com.auction.app.common.dto.BidDTO;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/** Controller for one auction room. */
public class AuctionRoomController {
    @FXML private Label productLabel;
    @FXML private Label auctionIdLabel;
    @FXML private Label endLabel;
    @FXML private Label statusLabel;
    @FXML private Label priceLabel;
    @FXML private Label messageLabel;
    @FXML private TextField bidderField;
    @FXML private TextField amountField;
    @FXML private Button bidButton;

    private AuctionViewData auction;
    private double latestPrice = Double.NEGATIVE_INFINITY;
    private Timeline deadlineTimer;
    private boolean active;

    public void openAuction(AuctionViewData selectedAuction, double currentPrice) {
        auction = selectedAuction;
        active = true;
        productLabel.setText(auction.getProductName());
        auctionIdLabel.setText("Mã phiên: " + auction.getAuctionId());
        endLabel.setText("Kết thúc: " + auction.getEndTime());
        priceLabel.setText(auction.getCurrentPrice());
        latestPrice = currentPrice;
        statusLabel.setText(auction.getStatus());
        bidButton.setDisable(!"ĐANG DIỄN RA".equals(auction.getStatus()));
        ClientSession.getInstance().setBidConsumer(
                bid -> Platform.runLater(() -> handleBid(bid)));
        ClientSession.getInstance().setAuctionEndConsumer(result -> Platform.runLater(() -> {
            if (active && auction.getAuctionId().equals(result.auctionId())) {
                bidButton.setDisable(true);
                statusLabel.setText("Đã kết thúc");
                messageLabel.setText(result.winnerBidderId() == null
                        ? "Phiên đấu giá kết thúc, không có người đặt giá."
                        : "Người thắng: " + result.winnerBidderId());
                deadlineTimer.stop();
            }
        }));
        deadlineTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> checkDeadline()));
        deadlineTimer.setCycleCount(Timeline.INDEFINITE);
        deadlineTimer.play();
        checkDeadline();
    }

    private void handleBid(BidDTO bid) {
        if (!active || !auction.getAuctionId().equals(bid.auctionId())) {
            return;
        }
        if (bid.amount() > latestPrice) {
            latestPrice = bid.amount();
            priceLabel.setText(String.format(Locale.forLanguageTag("vi-VN"),
                    "%,.0f đ", latestPrice));
        }
    }

    private void checkDeadline() {
        if (auction != null && !Instant.now().isBefore(auction.getEndsAt())) {
            bidButton.setDisable(true);
            statusLabel.setText("Đã đến thời điểm kết thúc");
            messageLabel.setText("Đang chờ máy chủ xác nhận kết quả.");
            deadlineTimer.stop();
        }
    }

    @FXML
    private void placeBid() {
        String bidderId = bidderField.getText().trim();
        if (bidderId.isEmpty()) {
            messageLabel.setText("Vui lòng nhập mã người đấu giá.");
            return;
        }
        final double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            messageLabel.setText("Vui lòng nhập giá hợp lệ.");
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            messageLabel.setText("Giá đặt phải là số hữu hạn lớn hơn 0.");
            return;
        }
        if (!Instant.now().isBefore(auction.getEndsAt())) {
            checkDeadline();
            return;
        }
        messageLabel.setText("Đang gửi giá...");
        CompletableFuture.runAsync(() -> {
            try {
                ClientSession.getInstance().placeBid(auction.getAuctionId(), bidderId, amount,
                        error -> Platform.runLater(() -> {
                            if (active) {
                                messageLabel.setText(error);
                            }
                        }));
            } catch (IOException | NumberFormatException exception) {
                Platform.runLater(() -> {
                    if (active) {
                        messageLabel.setText("Không thể gửi giá: " + exception.getMessage());
                    }
                });
            }
        });
    }

    @FXML
    private void backToList(ActionEvent event) throws IOException {
        active = false;
        deadlineTimer.stop();
        ClientSession.getInstance().setBidConsumer(null);
        ClientSession.getInstance().setAuctionEndConsumer(null);
        ViewNavigator.open("main-page.fxml", (Node) event.getSource());
    }
}
