package com.auction.app.common.model.product;

public abstract class Product {
    private final String id;
    private final String name;
    private final double startingPrice;

    protected Product(String id, String name, double startingPrice) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public abstract String getProductType();
}