package com.auction.app.common.model.user;

import com.auction.app.common.model.Bid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Người dùng có thể tham gia trả giá. */
public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    private final List<Bid> placedBids = new ArrayList<>();

    public Bidder(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public boolean canBid() {
        return true;
    }

    public synchronized void recordBid(Bid bid) {
        Objects.requireNonNull(bid, "bid cannot be null");
        placedBids.add(bid);
    }

    public synchronized List<Bid> getPlacedBids() {
        return Collections.unmodifiableList(new ArrayList<>(placedBids));
    }
}
