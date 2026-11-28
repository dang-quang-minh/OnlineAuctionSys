package com.auction.app.client.controller;

import com.auction.app.client.demoData.AuctionDtoMapper;
import com.auction.app.client.demoModel.AuctionViewData;
import com.auction.app.client.network.AuctionGateway;
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
import java.util.concurrent.CompletionException;

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
    private Label openAuctionsCountLabel;
    @FXML
    private Button manageProductsButton;

    @FXML
    private void initialize() {
        String username = AuctionGateway.getInstance().getCurrentUsername();
        String role = AuctionGateway.getInstance().getCurrentUserRole();
        if (role != null) {
            String prefix = switch (role.toUpperCase(java.util.Locale.ROOT)) {
                case "SELLER" -> "🏷 Người bán: ";
                case "ADMIN" -> "🛡 Quản trị: ";
                default -> "👤 Người mua: ";
            };
            welcomeLabel.setText(prefix + (username != null ? username : "") + " [" + role + "]");
        } else {
            welcomeLabel.setText("Dữ liệu từ máy chủ");
        }
        boolean isSeller = "SELLER".equalsIgnoreCase(role);
        if (manageProductsButton != null) {
            manageProductsButton.setVisible(isSeller);
            manageProductsButton.setManaged(isSeller);
        }

        AuctionGateway.getInstance().setNewBidListener(
                bid -> javafx.application.Platform.runLater(this::refreshAuctions));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("Xem");

            {
                viewButton.getStyleClass().add("btn-action-table");
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
        AuctionGateway.getInstance().disconnect();
        ViewNavigator.open("login-view.fxml", (Node) event.getSource());
    }

    @FXML
    private void handleToggleTheme(ActionEvent event) {
        com.auction.app.client.view.ThemeManager.toggleTheme(((Node) event.getSource()).getScene());
    }

    private void refreshAuctions() {
        messageLabel.setText("Đang tải dữ liệu từ máy chủ...");
        AuctionGateway.getInstance().requestAuctions()
                .whenComplete((auctions, error) ->
                        javafx.application.Platform.runLater(() -> {
                            if (error != null) {
                                messageLabel.setText("Không thể tải dữ liệu: "
                                        + errorMessage(error));
                            } else {
                                showAuctions(AuctionDtoMapper.toViewData(auctions));
                            }
                        }));
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private void showAuctions(List<AuctionViewData> auctions) {
        auctionTable.setItems(FXCollections.observableArrayList(auctions));
        if (openAuctionsCountLabel != null) {
            openAuctionsCountLabel.setText(String.valueOf(auctions.size()));
        }
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
    @FXML
    private void handleOpenSellerProducts(ActionEvent event) throws IOException {
        ViewNavigator.open("seller-product-view.fxml", (Node) event.getSource());
    }
}
