package com.auction.app.client.network;

import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.CreateAuctionRequestDTO;
import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.ProductDetailDTO;
import com.auction.app.common.dto.ProductInputDTO;
import com.auction.app.common.dto.ProductResultDTO;
import com.auction.app.common.dto.ProductUpdateDTO;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Phiên kết nối dùng chung cho các màn hình JavaFX.
 */
public final class ClientSession {

    private static final ClientSession INSTANCE = new ClientSession();

    private final ClientNetwork network = new ClientNetwork();
    private volatile Consumer<LoginResultDTO> loginConsumer;
    private volatile Consumer<RegisterResultDTO> registerConsumer;
    private volatile Consumer<List<AuctionDTO>> auctionConsumer;
    private volatile Consumer<BidDTO> bidConsumer;
    private volatile Consumer<String> errorConsumer;
    private volatile Consumer<List<ProductDetailDTO>> productListConsumer;
    private volatile Consumer<ProductResultDTO> productResultConsumer;
    private Consumer<ProductResultDTO> createAuctionConsumer;

    private volatile String currentUserId;
    private volatile String currentUserRole;
    private volatile String currentUsername;

    private ClientSession() {
        network.setMessageListener(this::handleMessage);
        network.setDisconnectListener(this::handleError);
    }

    public static ClientSession getInstance() {
        return INSTANCE;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void connect() throws IOException {
        if (!network.isConnected()) {
            String host = System.getProperty("auction.server.host", "localhost");
            int port = Integer.parseInt(
                    System.getProperty("auction.server.port", "5050"));
            network.connect(host, port);
        }
    }

    public void login(
            String username,
            String password,
            Consumer<LoginResultDTO> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.loginConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        this.currentUsername = username;
        connect();
        network.sendMessage(new NetworkMessage(
                NetworkMessage.LOGIN, new LoginDTO(username, password)));
    }

    public void register(
            RegisterRequestDTO request,
            Consumer<RegisterResultDTO> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.registerConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(
                NetworkMessage.REGISTER, request));
    }

    public void requestAuctions(
            Consumer<List<AuctionDTO>> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.auctionConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(
                NetworkMessage.AUCTION_LIST, "REQUEST"));
    }

    public void placeBid(
            String auctionId,
            String bidderId,
            double amount,
            Consumer<String> errorConsumer) throws IOException {
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(
                NetworkMessage.PLACE_BID,
                new BidRequestDTO(auctionId, bidderId, amount)));
    }

    public void setBidConsumer(Consumer<BidDTO> consumer) {
        bidConsumer = consumer;
    }

    public void disconnect() {
        network.disconnect();
        loginConsumer = null;
        registerConsumer = null;
        auctionConsumer = null;
        bidConsumer = null;
        currentUserId = null;
        currentUserRole = null;
        currentUsername = null;
    }

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case NetworkMessage.LOGIN_RESULT -> {
                if (message.getPayload() instanceof LoginResultDTO result
                        && loginConsumer != null) {
                    if (result.success()) {
                        this.currentUserId = result.userId();
                        this.currentUserRole = result.role();
                    }
                    loginConsumer.accept(result);
                    loginConsumer = null;
                } else {
                    handleError("Phản hồi đăng nhập không hợp lệ.");
                }
            }
            case NetworkMessage.REGISTER_RESULT -> {
                if (message.getPayload() instanceof RegisterResultDTO result
                        && registerConsumer != null) {
                    registerConsumer.accept(result);
                    registerConsumer = null;
                } else {
                    handleError("Phản hồi đăng ký không hợp lệ.");
                }
            }
            case NetworkMessage.AUCTION_LIST -> {
                if (message.getPayload() instanceof ArrayList<?> values
                        && auctionConsumer != null) {
                    List<AuctionDTO> auctions = values.stream()
                            .filter(AuctionDTO.class::isInstance)
                            .map(AuctionDTO.class::cast)
                            .toList();
                    auctionConsumer.accept(auctions);
                    auctionConsumer = null;
                } else {
                    handleError("Phản hồi danh sách phiên không hợp lệ.");
                }
            }
            case NetworkMessage.ERROR -> handleError(
                    String.valueOf(message.getPayload()));
            case NetworkMessage.NEW_BID -> {
                if (message.getPayload() instanceof BidDTO bid
                        && bidConsumer != null) {
                    bidConsumer.accept(bid);
                }
            }
            case NetworkMessage.PRODUCT_LIST_RESULT -> {
                if (message.getPayload() instanceof List<?> values && productListConsumer != null) {
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
                if (message.getPayload() instanceof ProductResultDTO result && productResultConsumer != null) {
                    productResultConsumer.accept(result);
                    productResultConsumer = null;
                } else {
                    handleError("Phản hồi thao tác sản phẩm không hợp lệ.");
                }
            }
            default -> handleError("Server trả về message không được hỗ trợ.");
        }
    }

    private void handleError(String message) {
        Consumer<String> consumer = errorConsumer;
        if (consumer != null) {
            consumer.accept(message);
        }
    }

    public void requestSellerProducts(
            Consumer<List<ProductDetailDTO>> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.productListConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(NetworkMessage.PRODUCT_LIST, null));
    }

    public void createProduct(
            ProductInputDTO input,
            Consumer<ProductResultDTO> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.productResultConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(NetworkMessage.PRODUCT_CREATE, input));
    }

    public void updateProduct(
            ProductUpdateDTO update,
            Consumer<ProductResultDTO> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.productResultConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(NetworkMessage.PRODUCT_UPDATE, update));
    }

    public void deleteProduct(
            String productId,
            Consumer<ProductResultDTO> resultConsumer,
            Consumer<String> errorConsumer) throws IOException {
        this.productResultConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
        connect();
        network.sendMessage(new NetworkMessage(NetworkMessage.PRODUCT_DELETE, productId));
    }

    public void createAuction(
            CreateAuctionRequestDTO request,
            Consumer<ProductResultDTO> onSuccess,
            Consumer<String> onError) throws IOException {
        this.createAuctionConsumer = onSuccess;
        this.errorConsumer = onError;
        connect();
        network.sendMessage(new NetworkMessage(NetworkMessage.CREATE_AUCTION, request));
    }
}
