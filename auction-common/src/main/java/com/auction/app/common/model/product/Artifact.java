package com.auction.app.common.model.product;

import java.io.Serial;

/**
 * Lớp đại diện cho sản phẩm là cổ vật.
 */
public class Artifact extends Product {
    @Serial
    private static final long serialVersionUID = 1L;

    private String origin;

    public Artifact(String productId, String sellerId, String name, double startingPrice, String origin) {
        super(productId, sellerId, name, startingPrice);
        this.origin = origin;
    }

    public Artifact(String id, String name, double startingPrice, String origin) {
        super(id, name, startingPrice);
        this.origin = origin;
    }

    @Override
    public String getProductType() {
        return "ARTIFACT";
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}


