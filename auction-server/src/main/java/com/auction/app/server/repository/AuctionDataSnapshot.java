package com.auction.app.server.repository;

import com.auction.app.common.model.Auction;
import com.auction.app.common.model.Product;
import com.auction.app.common.model.user.User;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Đối tượng đóng gói toàn bộ trạng thái dữ liệu của hệ thống đấu giá để tuần tự hóa.
 */
public class AuctionDataSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, User> users;
    private Map<String, Product> products;
    private Map<String, Auction> auctions;

    public AuctionDataSnapshot() {
        this.users = new HashMap<>();
        this.products = new HashMap<>();
        this.auctions = new HashMap<>();
    }

    public AuctionDataSnapshot(Map<String, User> users, Map<String, Product> products, Map<String, Auction> auctions) {
        this.users = users != null ? new HashMap<>(users) : new HashMap<>();
        this.products = products != null ? new HashMap<>(products) : new HashMap<>();
        this.auctions = auctions != null ? new HashMap<>(auctions) : new HashMap<>();
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public void setUsers(Map<String, User> users) {
        this.users = users;
    }

    public Map<String, Product> getProducts() {
        return products;
    }

    public void setProducts(Map<String, Product> products) {
        this.products = products;
    }

    public Map<String, Auction> getAuctions() {
        return auctions;
    }

    public void setAuctions(Map<String, Auction> auctions) {
        this.auctions = auctions;
    }
}
