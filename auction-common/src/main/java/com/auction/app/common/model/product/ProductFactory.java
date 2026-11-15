package com.auction.app.common.model.product;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ProductFactory {
    private ProductFactory() {
    }

    public static Product createProduct(String type, String id, String name, double price,
            Map<String, ?> attributes) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(attributes, "attributes");
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "ART" -> new Art(id, name, price, attribute(attributes, "artist", String.class));
            case "VEHICLE" -> new Vehicle(id, name, price, attribute(attributes, "manufacturingYear", Integer.class));
            case "ARTIFACT" -> new Artifact(id, name, price, attribute(attributes, "origin", String.class));
            default -> throw new IllegalArgumentException("Unknown product type: " + type);
        };
    }

    private static <T> T attribute(Map<String, ?> attributes, String key, Class<T> type) {
        Object value = attributes.get(key);
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Attribute " + key + " must be a " + type.getSimpleName());
        }
        return type.cast(value);
    }
}
