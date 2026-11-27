package com.auction.app.common.model.user;

import com.auction.app.common.model.product.Product;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Người dùng có thể đưa sản phẩm lên đấu giá. */
public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private final List<Product> products = new ArrayList<>();

    public Seller(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    @Override
    public boolean canSell() {
        return true;
    }

    public synchronized void addProduct(Product product) {
        Objects.requireNonNull(product, "product cannot be null");
        products.add(product);
    }

    public synchronized boolean removeProduct(String productId) {
        Objects.requireNonNull(productId, "productId cannot be null");
        return products.removeIf(product -> productId.equals(product.getId()));
    }

    public synchronized boolean ownsProduct(String productId) {
        if (productId == null) {
            return false;
        }
        return products.stream().anyMatch(product -> productId.equals(product.getId()));
    }

    public synchronized List<Product> getProducts() {
        return Collections.unmodifiableList(new ArrayList<>(products));
    }
}
