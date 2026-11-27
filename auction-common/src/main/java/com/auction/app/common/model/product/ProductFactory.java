package com.auction.app.common.model.product;


import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Tạo đúng loại sản phẩm từ loại và các thuộc tính riêng của sản phẩm. */
public final class ProductFactory {
    private ProductFactory() {
    }

    public static Product createProduct(String type, String productId, String sellerId, String name, double price,
                                        Map<String, ?> attributes) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(attributes, "attributes");
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "ART" -> new Art(
                    productId, sellerId, name, price, requiredAttribute(attributes, "artist", String.class));
            case "ARTIFACT" -> new Artifact(
                    productId, sellerId, name, price, requiredAttribute(attributes, "origin", String.class));
            case "VEHICLE" -> new Vehicle(
                    productId, sellerId, name, price,
                    requiredAttribute(attributes, "manufacturingYear", Integer.class));
            default -> throw new IllegalArgumentException("Loại sản phẩm không hỗ trợ: " + type);
        };
    }

    private static <T> T requiredAttribute(Map<String, ?> attributes, String key, Class<T> type) {
        Object value = attributes.get(key);
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Thuộc tính '" + key + "' phải có kiểu " + type.getSimpleName());
        }
        return type.cast(value);
    }

}
