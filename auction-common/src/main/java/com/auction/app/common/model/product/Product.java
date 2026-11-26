package com.auction.app.common.model.product;
import java.io.Serial;
import java.io.Serializable;

public abstract class Product implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String sellerId;
    private final String name;
    private final double startingPrice;

    protected Product(String productId, String sellerId, String name, double startingPrice) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId không được để trống");
        }
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("sellerId không được để trống");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }
        this.productId = productId;
        this.sellerId = sellerId;
        this.name = name;
        this.startingPrice = startingPrice;
    }

    public String getId() {
        return productId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public abstract String getProductType();
}