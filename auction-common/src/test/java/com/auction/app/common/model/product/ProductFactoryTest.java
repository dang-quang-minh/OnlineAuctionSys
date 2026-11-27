package com.auction.app.common.model.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductFactoryTest {

    @Test
    void createArtSuccess() {
        Product product = ProductFactory.createProduct(
                "art", "P-1", "S-1", "Mona Lisa", 5000.0, Map.of("artist", "Da Vinci"));
        Art art = assertInstanceOf(Art.class, product);
        assertEquals("P-1", art.getId());
        assertEquals("S-1", art.getSellerId());
        assertEquals("Mona Lisa", art.getName());
        assertEquals(5000.0, art.getStartingPrice());
        assertEquals("ART", art.getProductType());
        assertEquals("Da Vinci", art.getArtist());
    }

    @Test
    void createArtifactSuccess() {
        Product product = ProductFactory.createProduct(
                "ARTIFACT", "P-2", "S-1", "Trống đồng", 3000.0, Map.of("origin", "Đông Sơn"));
        Artifact artifact = assertInstanceOf(Artifact.class, product);
        assertEquals("ARTIFACT", artifact.getProductType());
        assertEquals("Đông Sơn", artifact.getOrigin());
    }

    @Test
    void createVehicleSuccess() {
        Product product = ProductFactory.createProduct(
                "Vehicle", "P-3", "S-1", "Vespa Cổ", 8000.0, Map.of("manufacturingYear", 1965));
        Vehicle vehicle = assertInstanceOf(Vehicle.class, product);
        assertEquals("VEHICLE", vehicle.getProductType());
        assertEquals(1965, vehicle.getManufacturingYear());
    }

    @Test
    void createProductRejectsUnsupportedType() {
        assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("ELECTRONICS", "P-4", "S-1", "Laptop", 1000.0, Map.of()));
    }

    @Test
    void createProductRejectsMissingOrWrongAttributeType() {
        assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("ART", "P-5", "S-1", "Tranh", 1000.0, Map.of()));
        assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct(
                        "VEHICLE", "P-6", "S-1", "Xe", 1000.0, Map.of("manufacturingYear", "1990")));
    }

    @Test
    void createProductRejectsNullTypeOrAttributes() {
        assertThrows(NullPointerException.class, () ->
                ProductFactory.createProduct(null, "P-7", "S-1", "Tên", 1000.0, Map.of()));
        assertThrows(NullPointerException.class, () ->
                ProductFactory.createProduct("ART", "P-8", "S-1", "Tên", 1000.0, null));
    }
}