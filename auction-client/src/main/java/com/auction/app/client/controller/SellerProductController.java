package com.auction.app.client.controller;

import com.auction.app.client.network.ClientSession;
import com.auction.app.client.view.ViewNavigator;
import com.auction.app.common.dto.CreateAuctionRequestDTO;
import com.auction.app.common.dto.ProductDetailDTO;
import com.auction.app.common.dto.ProductInputDTO;
import com.auction.app.common.dto.ProductResultDTO;
import com.auction.app.common.dto.ProductUpdateDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SellerProductController {

    @FXML
    private TableView<ProductDetailDTO> productTable;
    @FXML
    private TableColumn<ProductDetailDTO, String> idColumn;
    @FXML
    private TableColumn<ProductDetailDTO, String> typeColumn;
    @FXML
    private TableColumn<ProductDetailDTO, String> nameColumn;
    @FXML
    private TableColumn<ProductDetailDTO, String> priceColumn;
    @FXML
    private TableColumn<ProductDetailDTO, String> detailColumn;

    @FXML
    private Label formTitleLabel;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextField priceField;

    @FXML
    private VBox artistBox;
    @FXML
    private TextField artistField;
    @FXML
    private VBox originBox;
    @FXML
    private TextField originField;
    @FXML
    private VBox yearBox;
    @FXML
    private TextField yearField;

    @FXML
    private Button createButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label statusLabel;

    private ProductDetailDTO selectedProduct;

    @FXML
    private void initialize() {
        setupTable();
        setupTypeSelector();
        loadProducts();
    }

    private void setupTable() {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().productId()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(
                currencyFormat.format(data.getValue().startingPrice()) + " đ"));
        detailColumn.setCellValueFactory(data -> new SimpleStringProperty(formatSpecificDetail(data.getValue())));

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormForEdit(newVal);
            }
        });
    }

    private String formatSpecificDetail(ProductDetailDTO dto) {
        if (dto.type() == null) {
            return "";
        }
        return switch (dto.type().toUpperCase(Locale.ROOT)) {
            case "ART" -> "Tác giả: " + (dto.artist() != null ? dto.artist() : "");
            case "ARTIFACT" -> "Xuất xứ: " + (dto.origin() != null ? dto.origin() : "");
            case "VEHICLE" -> "Năm SX: " + (dto.manufacturingYear() != null ? dto.manufacturingYear() : "");
            default -> "";
        };
    }

    private void setupTypeSelector() {
        typeComboBox.setItems(FXCollections.observableArrayList("ART", "ARTIFACT", "VEHICLE"));
        typeComboBox.valueProperty().addListener((obs, oldType, newType) -> updateDynamicFields(newType));
        typeComboBox.setValue("ART");
    }

    private void updateDynamicFields(String type) {
        boolean isArt = "ART".equalsIgnoreCase(type);
        boolean isArtifact = "ARTIFACT".equalsIgnoreCase(type);
        boolean isVehicle = "VEHICLE".equalsIgnoreCase(type);

        artistBox.setVisible(isArt);
        artistBox.setManaged(isArt);

        originBox.setVisible(isArtifact);
        originBox.setManaged(isArtifact);

        yearBox.setVisible(isVehicle);
        yearBox.setManaged(isVehicle);
    }

    private void populateFormForEdit(ProductDetailDTO dto) {
        selectedProduct = dto;
        formTitleLabel.setText("Cập nhật sản phẩm: " + dto.productId());
        typeComboBox.setValue(dto.type());
        typeComboBox.setDisable(true); // Không đổi loại khi cập nhật

        nameField.setText(dto.name());
        priceField.setText(String.valueOf(dto.startingPrice()));
        artistField.setText(dto.artist() != null ? dto.artist() : "");
        originField.setText(dto.origin() != null ? dto.origin() : "");
        yearField.setText(dto.manufacturingYear() != null ? String.valueOf(dto.manufacturingYear()) : "");

        createButton.setDisable(true);
        updateButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    @FXML
    private void handleClearForm() {
        selectedProduct = null;
        productTable.getSelectionModel().clearSelection();
        formTitleLabel.setText("Thêm sản phẩm mới");
        typeComboBox.setDisable(false);
        typeComboBox.setValue("ART");

        nameField.clear();
        priceField.clear();
        artistField.clear();
        originField.clear();
        yearField.clear();

        createButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    @FXML
    private void handleCreate() {
        ProductInputDTO input = buildInputFromForm();
        if (input == null) {
            return;
        }

        statusLabel.setText("Đang tạo sản phẩm...");
        try {
            ClientSession.getInstance().createProduct(
                    input,
                    result -> Platform.runLater(() -> onMutationSuccess(result)),
                    error -> Platform.runLater(() -> statusLabel.setText("Lỗi: " + error)));
        } catch (IOException e) {
            statusLabel.setText("Lỗi kết nối: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedProduct == null) {
            return;
        }
        ProductInputDTO input = buildInputFromForm();
        if (input == null) {
            return;
        }

        statusLabel.setText("Đang cập nhật sản phẩm...");
        try {
            ClientSession.getInstance().updateProduct(
                    new ProductUpdateDTO(selectedProduct.productId(), input),
                    result -> Platform.runLater(() -> onMutationSuccess(result)),
                    error -> Platform.runLater(() -> statusLabel.setText("Lỗi: " + error)));
        } catch (IOException e) {
            statusLabel.setText("Lỗi kết nối: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedProduct == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm \"" + selectedProduct.name() + "\"?");
        confirm.setContentText("Thao tác này không thể hoàn tác.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        statusLabel.setText("Đang xóa sản phẩm...");
        try {
            ClientSession.getInstance().deleteProduct(
                    selectedProduct.productId(),
                    result -> Platform.runLater(() -> onMutationSuccess(result)),
                    error -> Platform.runLater(() -> statusLabel.setText("Lỗi: " + error)));
        } catch (IOException e) {
            statusLabel.setText("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void onMutationSuccess(ProductResultDTO result) {
        statusLabel.setText(result.message());
        if (result.success()) {
            handleClearForm();
            loadProducts();
        }
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        ViewNavigator.open("main-page.fxml", (Node) event.getSource());
    }

    private void loadProducts() {
        statusLabel.setText("Đang tải danh sách sản phẩm...");
        try {
            ClientSession.getInstance().requestSellerProducts(
                    products -> Platform.runLater(() -> updateTable(products)),
                    error -> Platform.runLater(() -> statusLabel.setText("Lỗi: " + error)));
        } catch (IOException e) {
            statusLabel.setText("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void updateTable(List<ProductDetailDTO> products) {
        productTable.setItems(FXCollections.observableArrayList(products));
        statusLabel.setText("Đang hiển thị " + products.size() + " sản phẩm.");
    }

    private ProductInputDTO buildInputFromForm() {
        String type = typeComboBox.getValue();
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) {
            statusLabel.setText("Vui lòng nhập tên sản phẩm.");
            return null;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                statusLabel.setText("Giá khởi điểm phải lớn hơn 0.");
                return null;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá khởi điểm không hợp lệ.");
            return null;
        }

        String artist = null;
        String origin = null;
        Integer year = null;

        switch (type) {
            case "ART" -> {
                artist = artistField.getText() != null ? artistField.getText().trim() : "";
                if (artist.isEmpty()) {
                    statusLabel.setText("Vui lòng nhập tên tác giả.");
                    return null;
                }
            }
            case "ARTIFACT" -> {
                origin = originField.getText() != null ? originField.getText().trim() : "";
                if (origin.isEmpty()) {
                    statusLabel.setText("Vui lòng nhập xuất xứ cổ vật.");
                    return null;
                }
            }
            case "VEHICLE" -> {
                try {
                    year = Integer.parseInt(yearField.getText().trim());
                } catch (NumberFormatException e) {
                    statusLabel.setText("Năm sản xuất phải là số nguyên hợp lệ.");
                    return null;
                }
            }
        }

        return new ProductInputDTO(type, name, price, artist, origin, year);
    }

    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setStyle(isError ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
            statusLabel.setText(message);
        }
    }

    @FXML
    private void handleCreateAuction() {
        ProductDetailDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Vui lòng chọn một sản phẩm trong bảng để tạo phiên đấu giá.", true);
            return;
        }

        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Mở phiên đấu giá");
        dialog.setHeaderText("Sản phẩm: " + selected.name()
                + " (Giá khởi điểm: " + selected.startingPrice() + ")");
        dialog.setContentText("Thời lượng đấu giá (phút):");

        dialog.showAndWait().ifPresent(input -> {
            long durationMinutes;
            try {
                durationMinutes = Long.parseLong(input.trim());
            } catch (NumberFormatException e) {
                showStatus("Thời lượng phải là số nguyên dương (tính bằng phút).", true);
                return;
            }
            if (durationMinutes <= 0) {
                showStatus("Thời lượng đấu giá phải lớn hơn 0 phút.", true);
                return;
            }
            try {
                ClientSession.getInstance().createAuction(
                        new CreateAuctionRequestDTO(selected.productId(), durationMinutes),
                        res -> Platform.runLater(() -> showStatus(res.message(), !res.success())),
                        err -> Platform.runLater(() -> showStatus(err, true))
                );
            } catch (IOException e) {
                showStatus("Lỗi gửi yêu cầu mở phiên: " + e.getMessage(), true);
            }
        });
    }
}