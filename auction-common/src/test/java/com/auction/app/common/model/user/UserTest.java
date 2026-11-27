package com.auction.app.common.model.user;

import com.auction.app.common.model.Bid;
import com.auction.app.common.model.product.Art;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void userAuthenticationValidatesPasswordCorrectly() {
        User user = new Bidder("u1", "alice", "secret123", "alice@example.com");
        assertEquals("u1", user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());

        assertTrue(user.authenticate("secret123"));
        assertFalse(user.authenticate("wrong"));
        assertFalse(user.authenticate(null));
    }

    @Test
    void userThrowsWhenRequiredFieldsAreNull() {
        assertThrows(NullPointerException.class, () -> new Bidder(null, "u", "p", "e@a.com"));
        assertThrows(NullPointerException.class, () -> new Bidder("id", null, "p", "e@a.com"));
        assertThrows(NullPointerException.class, () -> new Bidder("id", "u", null, "e@a.com"));
        assertThrows(NullPointerException.class, () -> new Bidder("id", "u", "p", null));
    }

    @Test
    void bidderRoleAndBidHistoryBehaveCorrectly() {
        Bidder bidder = new Bidder("b1", "bidder1", "pass", "b1@test.com");
        assertEquals("BIDDER", bidder.getRole());
        assertTrue(bidder.canBid());
        assertFalse(bidder.canSell());
        assertFalse(bidder.isAdmin());

        assertTrue(bidder.getPlacedBids().isEmpty());

        Bid bid = new Bid("bid-1", "b1", 500.0, Instant.now());
        bidder.recordBid(bid);

        assertEquals(1, bidder.getPlacedBids().size());
        assertEquals(500.0, bidder.getPlacedBids().get(0).getAmount());

        assertThrows(UnsupportedOperationException.class, () -> bidder.getPlacedBids().add(bid));
        assertThrows(NullPointerException.class, () -> bidder.recordBid(null));
    }

    @Test
    void sellerRoleAndInventoryBehaveCorrectly() {
        Seller seller = new Seller("s1", "seller1", "pass", "s1@test.com");
        assertEquals("SELLER", seller.getRole());
        assertTrue(seller.canSell());
        assertFalse(seller.canBid());
        assertFalse(seller.isAdmin());

        Art product = new Art("p1", "Bức tranh", 1000.0, "Họa sĩ A");
        seller.addProduct(product);

        assertTrue(seller.ownsProduct("p1"));
        assertFalse(seller.ownsProduct("p2"));
        assertFalse(seller.ownsProduct(null));
        assertEquals(1, seller.getProducts().size());

        assertTrue(seller.removeProduct("p1"));
        assertFalse(seller.ownsProduct("p1"));
        assertFalse(seller.removeProduct("p1"));

        assertThrows(NullPointerException.class, () -> seller.addProduct(null));
        assertThrows(NullPointerException.class, () -> seller.removeProduct(null));
    }

    @Test
    void adminRoleBehaveCorrectly() {
        Admin admin = new Admin("a1", "admin", "admin123", "admin@test.com");
        assertEquals("ADMIN", admin.getRole());
        assertTrue(admin.isAdmin());
        assertFalse(admin.canBid());
        assertFalse(admin.canSell());

        Bidder bidder = new Bidder("b1", "bidder", "pass", "b@test.com");
        assertTrue(bidder.isActive());

        admin.lockUser(bidder);
        assertFalse(bidder.isActive());

        admin.unlockUser(bidder);
        assertTrue(bidder.isActive());

        assertTrue(admin.canCancelAuction(null) == false || admin.canCancelAuction(null) == false);
        assertThrows(NullPointerException.class, () -> admin.lockUser(null));
        assertThrows(NullPointerException.class, () -> admin.unlockUser(null));
    }

    @Test
    void userModelsAreSerializable() throws Exception {
        Bidder bidder = new Bidder("b-serial", "serial_bidder", "p123", "b@serial.com");
        bidder.recordBid(new Bid("bid-s", "b-serial", 300.0, Instant.now()));

        Seller seller = new Seller("s-serial", "serial_seller", "p123", "s@serial.com");
        seller.addProduct(new Art("art-s", "Tranh thủy mặc", 1200.0, "Họa sĩ"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(bidder);
            oos.writeObject(seller);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            Bidder deserializedBidder = (Bidder) ois.readObject();
            assertNotNull(deserializedBidder);
            assertEquals("b-serial", deserializedBidder.getId());
            assertEquals(1, deserializedBidder.getPlacedBids().size());

            Seller deserializedSeller = (Seller) ois.readObject();
            assertNotNull(deserializedSeller);
            assertEquals("s-serial", deserializedSeller.getId());
            assertTrue(deserializedSeller.ownsProduct("art-s"));
        }
    }
}
