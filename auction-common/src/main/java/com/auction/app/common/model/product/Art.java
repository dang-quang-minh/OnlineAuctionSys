package com.auction.app.common.model.product;

public class Art extends Product {
    private final String artist;

    public Art(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice);
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public String getProductType() {
        return "ART";
    }
}
