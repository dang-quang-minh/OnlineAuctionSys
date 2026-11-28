package com.auction.app.server.network;

import com.auction.app.common.dto.CreateAuctionRequestDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.dto.ProductResultDTO;
import com.auction.app.common.model.Auction;
import com.auction.app.common.model.product.Product;
import com.auction.app.common.model.user.Seller;
import com.auction.app.common.model.user.User;
import com.auction.app.server.service.AuctionManager;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Xử lý tuần tự message của một client trên một worker riêng.
 */
final class ClientHandler implements Runnable, AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final MessageDispatcher dispatcher;
    private final AuctionManager auctionManager;
    private final ProductCatalogHandler productCatalogHandler;
    private final Consumer<ClientHandler> onClose;
    private final Consumer<ClientHandler> onReady;
    private final Object writeLock = new Object();

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile boolean open = true;
    private volatile User currentUser;

    ClientHandler(
            Socket socket,
            MessageDispatcher dispatcher,
            AuctionManager auctionManager,
            ProductCatalogHandler productCatalogHandler,
            Consumer<ClientHandler> onClose,
            Consumer<ClientHandler> onReady) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.productCatalogHandler =
                Objects.requireNonNull(productCatalogHandler, "productCatalogHandler");
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        this.onReady = Objects.requireNonNull(onReady, "onReady");
    }

    @Override
    public void run() {
        try (Socket managedSocket = socket) {
            output = new ObjectOutputStream(managedSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(managedSocket.getInputStream());
            onReady.accept(this);

            while (open) {
                Object value = input.readObject();
                if (!(value instanceof NetworkMessage message)) {
                    sendError("Message không hợp lệ.");
                    continue;
                }
                handle(message);
            }
        } catch (EOFException exception) {
            LOGGER.log(System.Logger.Level.DEBUG, "Client đã đóng kết nối.", exception);
        } catch (IOException | ClassNotFoundException exception) {
            if (open) {
                LOGGER.log(System.Logger.Level.INFO, "Lỗi kết nối client.", exception);
            }
        } finally {
            open = false;
            onClose.accept(this);
            closeStreams();
        }
    }

    private void handle(NetworkMessage message) throws IOException {
        switch (message.getType()) {
            case NetworkMessage.PRODUCT_LIST,
                 NetworkMessage.PRODUCT_CREATE,
                 NetworkMessage.PRODUCT_UPDATE,
                 NetworkMessage.PRODUCT_DELETE ->
                    send(productCatalogHandler.handle(message, currentUser));

            case NetworkMessage.CREATE_AUCTION,
                 NetworkMessage.AUCTION_CREATE ->
                    handleCreateAuction(message);

            default -> {
                NetworkMessage response = dispatcher.dispatch(message);
                updateSessionUser(response);
                if (response != null) {
                    send(response);
                }
            }
        }
    }

    private void updateSessionUser(NetworkMessage response) {
        if (response != null
                && response.getPayload() instanceof LoginResultDTO loginResult
                && loginResult.success()
                && loginResult.userId() != null) {
            currentUser = auctionManager.getUserById(loginResult.userId());
        }
    }

    boolean send(NetworkMessage message) {
        if (!open || message == null) {
            return false;
        }
        try {
            synchronized (writeLock) {
                if (output == null) {
                    return false;
                }
                output.writeObject(message);
                output.flush();
                output.reset();
            }
            return true;
        } catch (IOException exception) {
            close();
            return false;
        }
    }

    private void sendError(String message) {
        send(new NetworkMessage(
                NetworkMessage.ERROR,
                message == null || message.isBlank()
                        ? "Yêu cầu bị từ chối."
                        : message));
    }

    private void handleCreateAuction(NetworkMessage message) {
        String responseType = NetworkMessage.AUCTION_CREATE.equals(message.getType())
                ? NetworkMessage.AUCTION_CREATE_RESULT
                : NetworkMessage.CREATE_AUCTION_RESULT;

        if (currentUser == null
                || !currentUser.canSell()
                || !(currentUser instanceof Seller seller)) {
            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(
                            null,
                            false,
                            "Chỉ người bán (SELLER) mới có quyền tạo phiên đấu giá."));
            return;
        }

        if (!(message.getPayload() instanceof CreateAuctionRequestDTO request)) {
            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(null, false, "Dữ liệu tạo phiên không hợp lệ."));
            return;
        }

        if (request.durationMinutes() <= 0) {
            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(
                            null,
                            false,
                            "Thời lượng đấu giá phải lớn hơn 0 phút."));
            return;
        }

        Product product =
                productCatalogHandler.findOwnedProduct(request.productId(), seller);
        if (product == null) {
            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(
                            null,
                            false,
                            "Không tìm thấy sản phẩm thuộc quyền sở hữu của bạn."));
            return;
        }

        try {
            String auctionId = "AUC-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);
            Instant endsAt = Instant.now()
                    .plus(Duration.ofMinutes(request.durationMinutes()));

            Auction auction = new Auction(auctionId, product, seller, endsAt);
            auction.start();
            auctionManager.createAuction(auction);

            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(
                            auctionId,
                            true,
                            "Đã mở phiên đấu giá " + auctionId + " thành công."));
        } catch (RuntimeException exception) {
            sendCreateAuctionResult(
                    responseType,
                    new ProductResultDTO(
                            null,
                            false,
                            "Không thể tạo phiên: " + exception.getMessage()));
        }
    }

    private void sendCreateAuctionResult(
            String responseType,
            ProductResultDTO result) {
        send(new NetworkMessage(responseType, result));
    }

    @Override
    public void close() {
        open = false;
        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(
                    System.Logger.Level.DEBUG,
                    "Không thể đóng socket client.",
                    exception);
        }
        closeStreams();
    }

    private void closeStreams() {
        ObjectInputStream currentInput = input;
        ObjectOutputStream currentOutput = output;
        input = null;
        output = null;

        try {
            if (currentInput != null) {
                currentInput.close();
            }
        } catch (IOException exception) {
            LOGGER.log(
                    System.Logger.Level.DEBUG,
                    "Không thể đóng input stream.",
                    exception);
        }

        try {
            if (currentOutput != null) {
                currentOutput.close();
            }
        } catch (IOException exception) {
            LOGGER.log(
                    System.Logger.Level.DEBUG,
                    "Không thể đóng output stream.",
                    exception);
        }
    }
}