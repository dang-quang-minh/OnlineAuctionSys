package com.auction.app.server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.ProductDetailDTO;
import com.auction.app.common.dto.ProductInputDTO;
import com.auction.app.common.dto.ProductResultDTO;
import com.auction.app.common.dto.ProductUpdateDTO;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Bidder;
import com.auction.app.common.model.user.Seller;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductCatalogHandlerTest {

    private Map<String, Product> store;
    private AtomicInteger saveCount;
    private ProductCatalogHandler handler;
    private Seller sellerA;
    private Seller sellerB;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        store = new ConcurrentHashMap<>();
        saveCount = new AtomicInteger(0);
        handler = new ProductCatalogHandler(store, saveCount::incrementAndGet);
        sellerA = new Seller("seller-a", "sellera", "pass123", "a@auction.com");
        sellerB = new Seller("seller-b", "sellerb", "pass123", "b@auction.com");
        bidder = new Bidder("bidder-1", "bidder1", "pass123", "bid@auction.com");
    }

    @Test
    void rejectUnauthenticatedOrBidderUser() {
        NetworkMessage listReq = new NetworkMessage(NetworkMessage.PRODUCT_LIST, "");

        NetworkMessage unauthRes = handler.handle(listReq, null);
        assertEquals(NetworkMessage.ERROR, unauthRes.getType());

        NetworkMessage bidderRes = handler.handle(listReq, bidder);
        assertEquals(NetworkMessage.ERROR, bidderRes.getType());
    }

    @Test
    void fullCrudLifecycleForOwnerSeller() {
        // 1. Create
        ProductInputDTO createInput = new ProductInputDTO(
                "ART", "Bức họa mùa thu", 2500.0, "Tô Ngọc Vân", null, null);
        NetworkMessage createRes = handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_CREATE, createInput), sellerA);
        assertEquals(NetworkMessage.PRODUCT_RESULT, createRes.getType());
        ProductResultDTO created = assertInstanceOf(ProductResultDTO.class, createRes.getPayload());
        assertTrue(created.success());
        assertNotNull(created.productId());
        assertEquals(1, saveCount.get());
        assertTrue(sellerA.ownsProduct(created.productId()));

        // 2. List
        NetworkMessage listRes = handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_LIST, ""), sellerA);
        assertEquals(NetworkMessage.PRODUCT_LIST_RESULT, listRes.getType());
        List<?> items = assertInstanceOf(List.class, listRes.getPayload());
        assertEquals(1, items.size());
        ProductDetailDTO detail = assertInstanceOf(ProductDetailDTO.class, items.get(0));
        assertEquals("Bức họa mùa thu", detail.name());
        assertEquals("Tô Ngọc Vân", detail.artist());

        // 3. Update
        ProductInputDTO updateInput = new ProductInputDTO(
                "ART", "Bức họa mùa thu (Bản phục chế)", 3200.0, "Tô Ngọc Vân", null, null);
        NetworkMessage updateRes = handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_UPDATE,
                        new ProductUpdateDTO(created.productId(), updateInput)),
                sellerA);
        ProductResultDTO updated = assertInstanceOf(ProductResultDTO.class, updateRes.getPayload());
        assertTrue(updated.success());
        assertEquals(3200.0, store.get(created.productId()).getStartingPrice());
        assertEquals(2, saveCount.get());

        // 4. Delete
        NetworkMessage deleteRes = handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_DELETE, created.productId()), sellerA);
        ProductResultDTO deleted = assertInstanceOf(ProductResultDTO.class, deleteRes.getPayload());
        assertTrue(deleted.success());
        assertTrue(store.isEmpty());
        assertFalse(sellerA.ownsProduct(created.productId()));
        assertEquals(3, saveCount.get());
    }

    @Test
    void preventOtherSellerFromUpdatingOrDeletingProduct() {
        ProductInputDTO createInput = new ProductInputDTO(
                "VEHICLE", "Cub 81", 1500.0, null, null, 1985);
        ProductResultDTO created = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_CREATE, createInput), sellerA).getPayload();

        ProductInputDTO hackInput = new ProductInputDTO(
                "VEHICLE", "Cub 81 Hacked", 1.0, null, null, 1985);
        ProductResultDTO updateRes = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_UPDATE,
                        new ProductUpdateDTO(created.productId(), hackInput)),
                sellerB).getPayload();
        assertFalse(updateRes.success());

        ProductResultDTO deleteRes = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_DELETE, created.productId()), sellerB).getPayload();
        assertFalse(deleteRes.success());
        assertEquals(1, store.size());
    }

    @Test
    void rejectInvalidPriceOrTypeChangeOnUpdate() {
        ProductInputDTO invalidInput = new ProductInputDTO(
                "ARTIFACT", "Bình gốm", -100.0, null, "Bát Tràng", null);
        ProductResultDTO createRes = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_CREATE, invalidInput), sellerA).getPayload();
        assertFalse(createRes.success());

        ProductInputDTO validInput = new ProductInputDTO(
                "ARTIFACT", "Bình gốm", 1200.0, null, "Bát Tràng", null);
        ProductResultDTO created = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_CREATE, validInput), sellerA).getPayload();

        ProductInputDTO changeTypeInput = new ProductInputDTO(
                "ART", "Bình gốm", 1200.0, "Nghệ nhân", null, null);
        ProductResultDTO updateRes = (ProductResultDTO) handler.handle(
                new NetworkMessage(NetworkMessage.PRODUCT_UPDATE,
                        new ProductUpdateDTO(created.productId(), changeTypeInput)),
                sellerA).getPayload();
        assertFalse(updateRes.success());
    }
}