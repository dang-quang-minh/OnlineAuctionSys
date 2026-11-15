package com.auction.app.common.model.product;

public class Artifact extends Product {
    private final String origin;

    public Artifact(String id, String name, double startingPrice, String origin) {
        super(id, name, startingPrice);
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    @Override
    public String getProductType() {
        return "ARTIFACT";
    }
}
