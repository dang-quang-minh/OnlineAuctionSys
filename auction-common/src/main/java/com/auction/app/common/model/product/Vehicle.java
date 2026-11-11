package com.auction.app.common.model.product;

public class Vehicle extends Product {
    private final int manufacturingYear;

    public Vehicle(String id, String name, double startingPrice, int manufacturingYear) {
        super(id, name, startingPrice);
        this.manufacturingYear = manufacturingYear;
    }

    public int getManufacturingYear() {
        return manufacturingYear;
    }

    @Override
    public String getProductType() {
        return "VEHICLE";
    }
}