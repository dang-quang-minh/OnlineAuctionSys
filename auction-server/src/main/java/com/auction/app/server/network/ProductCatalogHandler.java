package com.auction.app.server.network;

import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.ProductDetailDTO;
import com.auction.app.common.dto.ProductInputDTO;
import com.auction.app.common.dto.ProductResultDTO;
import com.auction.app.common.dto.ProductUpdateDTO;
import com.auction.app.common.model.product.Art;
import com.auction.app.common.model.product.Artifact;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.product.ProductFactory;
import com.auction.app.common.model.product.Vehicle;
import com.auction.app.common.model.user.Seller;
import com.auction.app.common.model.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Điều phối xử lý các thông điệp danh mục sản phẩm
 */
public class ProductCatalogHandler {
    private final Map<String, Product> productStore;
    private final Runnable onDataChanged;

    // Constructor mặc định dùng cho Unit Test và chạy độc lập khi chưa tích hợp lưu file:
    // - ConcurrentHashMap: tạo kho lưu tạm trên RAM, an toàn đa luồng khi nhiều Client kết nối.
    // - () -> { }: hàm callback lưu file rỗng (no-op) để tránh lỗi NullPointerException.
    public ProductCatalogHandler() {
        this(new ConcurrentHashMap<>(), () -> { });
    }
    public ProductCatalogHandler(Map<String, Product> productStore, Runnable onDataChanged) {
        this.productStore = Objects.requireNonNull(productStore, "productStore");
        this.onDataChanged = Objects.requireNonNull(onDataChanged, "onDataChanged");
    }

    public NetworkMessage handle(NetworkMessage message, User currentUser) {
        //kiểm tra tính hợp lệ đầu vào
        if (message == null) {
            return new NetworkMessage(NetworkMessage.ERROR, "Message không được để trống.");
        }

        //Xác thực quyền bán hàng
        if (currentUser == null || !currentUser.canSell() || !(currentUser instanceof Seller seller)) {
            return new NetworkMessage(NetworkMessage.ERROR, "Bạn không có quyền quản lý sản phẩm.");
        }

        return switch (message.getType()) {
            case NetworkMessage.PRODUCT_LIST -> handleList(seller);
            case NetworkMessage.PRODUCT_CREATE -> handleCreate(message, seller);
            case NetworkMessage.PRODUCT_UPDATE -> handleUpdate(message, seller);
            case NetworkMessage.PRODUCT_DELETE -> handleDelete(message, seller);
            default -> new NetworkMessage(
                    NetworkMessage.ERROR, "Loại thông điệp sản phẩm không hỗ trợ: " + message.getType()
            );
        };
    }

    public NetworkMessage handleList(Seller seller) {
        ArrayList<ProductDetailDTO> list = new ArrayList<>();
        for (Product product : productStore.values()) {
            if (seller.getId().equals(product.getSellerId())) {
                list.add(toDetailDTO(product));
            }
        }
        return new NetworkMessage(NetworkMessage.PRODUCT_LIST_RESULT, list);
    }

    private ProductDetailDTO toDetailDTO(Product product) {
        String artist = null;
        String origin = null;
        Integer manufacturingYear = null;

        if (product instanceof Art art) {
            artist = art.getArtist();
        } else if (product instanceof Artifact artifact) {
            origin = artifact.getOrigin();
        } else if (product instanceof Vehicle vehicle) {
            manufacturingYear = vehicle.getManufacturingYear();
        }

        return new ProductDetailDTO(
                product.getId(),
                product.getSellerId(),
                product.getProductType(),
                product.getName(),
                product.getStartingPrice(),
                artist,
                origin,
                manufacturingYear
        );
    }

    private NetworkMessage handleCreate(NetworkMessage message, Seller seller) {
        if (!(message.getPayload() instanceof ProductInputDTO input)) {
            return new NetworkMessage(NetworkMessage.ERROR, "Payload PRODUCT_CREATE không hợp lệ");
        }
        try {
            String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> attributes = buildAttributes(input);
            Product product = ProductFactory.createProduct(
                    input.type(),
                    productId,
                    seller.getId(),
                    input.name(),
                    input.startingPrice(),
                    attributes
            );
            productStore.put(product.getId(), product);
            seller.addProduct(product);
            onDataChanged.run();
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(product.getId(), true, "Thêm sản phẩm thành công"));
        } catch (IllegalArgumentException e) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO("", false, e.getMessage()));
        }
    }

    private Map<String, Object> buildAttributes(ProductInputDTO input) {
        Map<String, Object> attributes = new HashMap<>();
        if (input.artist() != null) {
            attributes.put("artist", input.artist());
        }
        if (input.origin() != null) {
            attributes.put("origin", input.origin());
        }
        if (input.manufacturingYear() != null) {
            attributes.put("manufacturingYear", input.manufacturingYear());
        }
        return attributes;
    }

    private NetworkMessage handleUpdate(NetworkMessage message, Seller seller) {
        if (!(message.getPayload() instanceof ProductUpdateDTO updateDto) || updateDto.product() == null) {
            return new NetworkMessage(NetworkMessage.ERROR, "Payload PRODUCT UPDATE không hợp lệ");
        }
        String productId = updateDto.productId();
        Product existing = productStore.get(productId);
        if (existing == null) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId == null ? "" : productId, false, "Không tìm thấy sản phẩm."));
        }
        if (!seller.ownsProduct(productId)) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, false, "Bạn không có quyền sửa sản phẩm của người khác."));
        }
        ProductInputDTO input = updateDto.product();
        if (input.type() != null && !existing.getProductType().equalsIgnoreCase(input.type().trim())) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, false, "Không được phép thay đổi loại sản phẩm."));
        }
        try {
            Product updated = ProductFactory.createProduct(
                    existing.getProductType(),
                    productId,
                    seller.getId(),
                    input.name(),
                    input.startingPrice(),
                    buildAttributes(input)
            );
            productStore.put(productId, updated);
            seller.removeProduct(productId);
            seller.addProduct(updated);
            onDataChanged.run();
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, true, "Cập nhật sản phẩm thành công."));
        } catch (IllegalArgumentException e) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, false, e.getMessage()));
        }
    }


    private NetworkMessage handleDelete(NetworkMessage message, Seller seller) {
        if (!(message.getPayload() instanceof String productId) || productId.isBlank()) {
            return new NetworkMessage(NetworkMessage.ERROR, "Payload PRODUCT_DELETE không hợp lệ.");
        }
        Product existing = productStore.get(productId);
        if (existing == null) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, false, "Không tìm thấy sản phẩm."));
        }
        if (!seller.ownsProduct(productId)) {
            return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                    new ProductResultDTO(productId, false, "Bạn không có quyền xóa sản phẩm của người khác."));
        }
        productStore.remove(productId);
        seller.removeProduct(productId);
        onDataChanged.run();
        return new NetworkMessage(NetworkMessage.PRODUCT_RESULT,
                new ProductResultDTO(productId, true, "Xóa sản phẩm thành công."));
    }
    public Product findOwnedProduct(String productId, Seller seller) {
        if (productId == null || productId.isBlank() || seller == null) {
            return null;
        }
        Product product = productStore.get(productId);
        if (product == null || !seller.getId().equals(product.getSellerId())) {
            return null;
        }
        return product;
    }
}
