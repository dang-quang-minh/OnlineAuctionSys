package com.auction.app.server.network;

import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.server.service.AuctionManager;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Xử lý tuần tự message của một client trên một worker riêng.
 */
final class ClientHandler implements Runnable, AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AccountHandler accountHandler;
    private final Consumer<ClientHandler> onClose;
    private final Consumer<ClientHandler> onReady;
    private final Object writeLock = new Object();

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile boolean open = true;

    ClientHandler(
            Socket socket,
            AuctionManager auctionManager,
            AccountHandler accountHandler,
            Consumer<ClientHandler> onClose,
            Consumer<ClientHandler> onReady) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.accountHandler = Objects.requireNonNull(accountHandler, "accountHandler");
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
                LOGGER.log(System.Logger.Level.INFO,
                        "Lỗi kết nối client.", exception);
            }
        } finally {
            open = false;
            onClose.accept(this);
            closeStreams();
        }
    }

    private void handle(NetworkMessage message) throws IOException {
        switch (message.getType()) {
            case NetworkMessage.LOGIN, NetworkMessage.REGISTER -> {
                NetworkMessage response = accountHandler.handle(message);
                send(response);
            }
            case NetworkMessage.AUCTION_LIST -> send(new NetworkMessage(
                    NetworkMessage.AUCTION_LIST,
                    ServerNetwork.activeAuctionDtos(auctionManager)));
            case NetworkMessage.PLACE_BID -> handleBid(message);
            default -> sendError("Loại message không được hỗ trợ: " + message.getType());
        }
    }

    private void handleBid(NetworkMessage message) throws IOException {
        if (!(message.getPayload() instanceof BidRequestDTO request)) {
            sendError("Payload PLACE_BID không hợp lệ.");
            return;
        }
        try {
            auctionManager.processBid(request.auctionId(), request);
        } catch (InvalidBidException | AuctionClosedException
                 | IllegalArgumentException | IllegalStateException exception) {
            sendError(exception.getMessage());
        }
    }

    boolean send(NetworkMessage message) {
        if (!open) {
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

    private void sendError(String message) throws IOException {
        send(new NetworkMessage(NetworkMessage.ERROR,
                message == null || message.isBlank() ? "Yêu cầu bị từ chối." : message));
    }

    @Override
    public void close() {
        open = false;
        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Không thể đóng socket client.", exception);
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
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Không thể đóng input stream.", exception);
        }
        try {
            if (currentOutput != null) {
                currentOutput.close();
            }
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Không thể đóng output stream.", exception);
        }
    }
}
