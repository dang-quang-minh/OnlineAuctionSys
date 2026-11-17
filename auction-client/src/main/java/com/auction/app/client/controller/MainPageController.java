package com.auction.app.client.controller;

import com.auction.app.client.data.AuctionDtoMapper;
import com.auction.app.client.data.DemoAuctionData;
import com.auction.app.client.model.AuctionViewData;
import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.dto.NetworkMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class MainPageController {
    private static final DateTimeFormatter END_TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.systemDefault());

    @FXML
    private TableView<AuctionViewData> auctionTable;
    @FXML
    private TableColumn<AuctionViewData, String> actionColumn;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        welcomeLabel.setText("Xin chào! Dữ liệu demo");
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("Xem");

            {
                viewButton.setOnAction(event -> showAuction(getTableView()
                        .getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
        refreshAuctions();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshAuctions();
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/auction/app/client/view/login-view.fxml"));
        Scene loginScene = (Scene) loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(loginScene);
    }

    private void refreshAuctions() {
        List<AuctionViewData> auctions = DemoAuctionData.load();
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
        messageLabel.setText(auctions.isEmpty()
                ? "Hiện chưa có phiên đấu giá nào."
                : "Đang hiển thị " + auctions.size() + " phiên đấu giá demo.");
    }

    /**
     * Entry point for the networking layer once the server sends AUCTION_LIST.
     * The listener may run off the JavaFX application thread.
     */
    public void handleNetworkMessage(NetworkMessage message) {
        if (message == null) {
            return;
        }
        switch (message.getType()) {
            case NetworkMessage.AUCTION_LIST -> showServerAuctions(message);
            case NetworkMessage.ERROR -> showMessage(String.valueOf(message.getPayload()));
            default -> {
                // LOGIN_RESULT is handled by the login screen; other messages
                // are not relevant to the auction list yet.
            }
        }
    }

    private void showServerAuctions(NetworkMessage message) {
        if (!(message.getPayload() instanceof ArrayList<?> payload)) {
            showMessage("Dữ liệu danh sách phiên từ máy chủ không hợp lệ.");
            return;
        }
        List<AuctionDTO> auctions = new ArrayList<>();
        for (Object item : payload) {
            if (!(item instanceof AuctionDTO auction)) {
                showMessage("Dữ liệu danh sách phiên từ máy chủ không hợp lệ.");
                return;
            }
            auctions.add(auction);
        }
        List<AuctionViewData> rows = AuctionDtoMapper.toViewData(auctions);
        Platform.runLater(() -> {
            auctionTable.setItems(FXCollections.observableArrayList(rows));
            messageLabel.setText(rows.isEmpty()
                    ? "Hiện chưa có phiên đấu giá nào."
                    : "Đang hiển thị " + rows.size() + " phiên từ máy chủ.");
        });
    }

    private void showMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    private void showAuction(AuctionViewData auction) {
        messageLabel.setText(String.format(
                "Phiên %s | %s | Người bán: %s | Giá khởi điểm: %s | "
                        + "Giá hiện tại: %s | %d lượt trả giá | Kết thúc: %s | %s",
                auction.getAuctionId(), auction.getProductName(), auction.getSeller(),
                auction.getStartingPrice(), auction.getCurrentPrice(), auction.getBidCount(),
                END_TIME_FORMAT.format(auction.getEndsAt()), auction.getDescription()));
    }
}
