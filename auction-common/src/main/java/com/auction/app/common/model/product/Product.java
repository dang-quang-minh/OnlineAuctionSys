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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (startingPrice < 0 || !Double.isFinite(startingPrice)) {
            throw new IllegalArgumentException("Giá khởi điểm phải là số hữu hạn không âm");
        }
        this.productId = productId;
        this.sellerId = sellerId != null ? sellerId : "unknown-seller";
        this.name = name;
        this.startingPrice = startingPrice;
    }

    protected Product(String id, String name, double startingPrice) {
        this(id, "unknown-seller", name, startingPrice);
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