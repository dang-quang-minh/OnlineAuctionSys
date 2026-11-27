package com.auction.app.common.model.product;

import java.io.Serial;

/**
 * Lớp đại diện cho sản phẩm là tác phẩm nghệ thuật.
 */
public class Art extends Product {
    @Serial
    private static final long serialVersionUID = 1L;

    // Họa sĩ sáng tác
    private String artist;

    public Art(String productId, String sellerId, String name, double startingPrice, String artist) {
        super(productId, sellerId, name, startingPrice);
        this.artist = artist;
    }

    public Art(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice);
        this.artist = artist;
    }

    @Override
    public String getProductType() {
        return "ART";
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}


