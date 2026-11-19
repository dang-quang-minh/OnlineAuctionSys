package com.auction.app.client.controller;

import com.auction.app.client.demoData.AuctionDtoMapper;
import com.auction.app.client.demoModel.AuctionViewData;
import com.auction.app.client.network.ClientSession;
import com.auction.app.client.view.ViewNavigator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.List;
import java.util.List;

public class MainPageController {
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
        welcomeLabel.setText("Dữ liệu từ máy chủ");
        ClientSession.getInstance().setBidConsumer(
                bid -> javafx.application.Platform.runLater(this::refreshAuctions));
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
        ClientSession.getInstance().disconnect();
        ViewNavigator.open("login-view.fxml", (Node) event.getSource());
    }

    private void refreshAuctions() {
        messageLabel.setText("Đang tải dữ liệu từ máy chủ...");
        try {
            ClientSession.getInstance().requestAuctions(
                    auctions -> javafx.application.Platform.runLater(
                            () -> showAuctions(AuctionDtoMapper.toViewData(auctions))),
                    error -> javafx.application.Platform.runLater(
                            () -> messageLabel.setText(error)));
        } catch (IOException | NumberFormatException exception) {
            messageLabel.setText("Không thể tải dữ liệu: " + exception.getMessage());
        }
    }

    private void showAuctions(List<AuctionViewData> auctions) {
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
        messageLabel.setText(auctions.isEmpty()
                ? "Máy chủ chưa có phiên đấu giá đang hoạt động."
                : "Đang hiển thị " + auctions.size() + " phiên từ máy chủ.");
    }

    private void showAuction(AuctionViewData auction) {
        messageLabel.setText(String.format(
                "Phiên %s | %s | Người bán: %s | Giá khởi điểm: %s | "
                        + "Giá hiện tại: %s | %d lượt trả giá | Kết thúc: %s | %s",
                auction.getAuctionId(), auction.getProductName(), auction.getSeller(),
                auction.getStartingPrice(), auction.getCurrentPrice(), auction.getBidCount(),
                auction.getEndTime(), auction.getDescription()));
    }
}
