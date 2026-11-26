package com.auction.app.client.network;

import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.dto.AuctionEndDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.LoginDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
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
    private volatile Consumer<List<AuctionDTO>> auctionConsumer;
    private volatile Consumer<BidDTO> bidConsumer;
    private volatile Consumer<AuctionEndDTO> auctionEndConsumer;
    private volatile Consumer<String> errorConsumer;

    private ClientSession() {
        network.setMessageListener(this::handleMessage);
        network.setDisconnectListener(this::handleError);
    }

    public static ClientSession getInstance() {
        return INSTANCE;
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
        connect();
        network.sendMessage(new NetworkMessage(
                NetworkMessage.LOGIN, new LoginDTO(username, password)));
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

    public void setAuctionEndConsumer(Consumer<AuctionEndDTO> consumer) {
        auctionEndConsumer = consumer;
    }

    public void disconnect() {
        network.disconnect();
        loginConsumer = null;
        auctionConsumer = null;
        bidConsumer = null;
        auctionEndConsumer = null;
    }

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case NetworkMessage.LOGIN_RESULT -> {
                if (message.getPayload() instanceof LoginResultDTO result
                        && loginConsumer != null) {
                    loginConsumer.accept(result);
                    loginConsumer = null;
                } else {
                    handleError("Phản hồi đăng nhập không hợp lệ.");
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
            case NetworkMessage.AUCTION_ENDED -> {
                if (message.getPayload() instanceof AuctionEndDTO result
                        && auctionEndConsumer != null) {
                    auctionEndConsumer.accept(result);
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
}
