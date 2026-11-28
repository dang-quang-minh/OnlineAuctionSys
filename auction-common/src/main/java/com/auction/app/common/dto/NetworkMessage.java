package com.auction.app.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class NetworkMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_RESULT = "LOGIN_RESULT";
    public static final String REGISTER = "REGISTER";
    public static final String REGISTER_RESULT = "REGISTER_RESULT";
    public static final String AUCTION_LIST = "AUCTION_LIST";
    public static final String AUCTION_DETAIL = "AUCTION_DETAIL";
    public static final String AUCTION_DETAIL_RESULT = "AUCTION_DETAIL_RESULT";
    public static final String AUCTION_CREATE = "AUCTION_CREATE";
    public static final String AUCTION_CREATE_RESULT = "AUCTION_CREATE_RESULT";
    public static final String PLACE_BID = "PLACE_BID";
    public static final String BID_RESULT = "BID_RESULT";
    public static final String NEW_BID = "NEW_BID";
            case NetworkMessage.BID_RESULT -> {
                if (message.getPayload() instanceof BidDTO bid) {
                    Consumer<BidDTO> consumer = bidResultConsumer;
                    bidResultConsumer = null;
                    if (consumer != null) {
                        consumer.accept(bid);
                    }
                } else {
                    handleError("Phản hồi đặt giá không hợp lệ.");
                }
            }
            case NetworkMessage.AUCTION_ENDED -> {
                Consumer<NetworkMessage> consumer = auctionEndedConsumer;
                if (consumer != null) {
                    consumer.accept(message);
                }
            }
            case NetworkMessage.PRODUCT_LIST_RESULT -> {
                if (message.getPayload() instanceof List<?> values
                        && productListConsumer != null) {
                    List<ProductDetailDTO> products = values.stream()
                            .filter(ProductDetailDTO.class::isInstance)
                            .map(ProductDetailDTO.class::cast)
                            .toList();
                    productListConsumer.accept(products);
                    productListConsumer = null;
                } else {
                    handleError("Phản hồi danh sách sản phẩm không hợp lệ.");
                }
            }
            case NetworkMessage.PRODUCT_RESULT -> {
                if (message.getPayload() instanceof ProductResultDTO result
                        && productResultConsumer != null) {
                    productResultConsumer.accept(result);
                    productResultConsumer = null;
                } else {
                    handleError("Phản hồi thao tác sản phẩm không hợp lệ.");
                }
            }
    public static final String PRODUCT_LIST = "PRODUCT_LIST";
    public static final String PRODUCT_LIST_RESULT = "PRODUCT_LIST_RESULT";
    public static final String PRODUCT_CREATE = "PRODUCT_CREATE";
    public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";
    public static final String PRODUCT_RESULT = "PRODUCT_RESULT";
    public static final String ERROR = "ERROR";
    public static final String CREATE_AUCTION = "CREATE_AUCTION";
    public static final String CREATE_AUCTION_RESULT = "CREATE_AUCTION_RESULT";

    private final String type;
    private final Serializable payload;

    public NetworkMessage(String type, Serializable payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public String getType() {
        return type;
    }

    public Serializable getPayload() {
        return payload;
    }
}
