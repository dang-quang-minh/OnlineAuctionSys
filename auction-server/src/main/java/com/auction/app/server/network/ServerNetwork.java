package com.auction.app.server.network;

import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.NetworkMessage;
import com.auction.app.common.exception.AuctionClosedException;
import com.auction.app.common.exception.InvalidBidException;
import com.auction.app.common.model.Auction;
import com.auction.app.server.service.AuctionManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import com.auction.app.common.observer.Observer;

/**
 * Cổng mạng của server, chịu trách nhiệm nhận kết nối và chuyển tiếp message.
 */
public final class ServerNetwork implements AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(ServerNetwork.class.getName());

    private final int requestedPort;
    private final AuctionManager auctionManager;
    private final AccountHandler accountHandler;
    private final MessageDispatcher dispatcher;
    private final ExecutorService clientExecutor;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Observer networkObserver = this::broadcast;

    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private Thread acceptThread;

    /**
     * Tạo server dùng AuctionManager và AccountService mặc định của ứng dụng.
     *
     * @param port cổng lắng nghe, hoặc 0 để hệ điều hành cấp cổng trống
     */
    public ServerNetwork(int port) {
        this(port, AuctionManager.getInstance(),
                new AccountHandler(new com.auction.app.server.service.AccountService()));
    }

    /**
     * Constructor cho ứng dụng và kiểm thử có thể cung cấp manager riêng.
     *
     * @param port cổng lắng nghe
     * @param auctionManager manager xử lý nghiệp vụ
     */
    public ServerNetwork(int port, AuctionManager auctionManager) {
        this(port, auctionManager,
                new AccountHandler(new com.auction.app.server.service.AccountService()));
    }

    /**
     * Constructor cung cấp đầy đủ cả AuctionManager và AccountHandler.
     *
     * @param port cổng lắng nghe
     * @param auctionManager manager xử lý nghiệp vụ
     * @param accountHandler handler xử lý tài khoản
     */
    public ServerNetwork(int port, AuctionManager auctionManager, AccountHandler accountHandler) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Cổng phải nằm trong khoảng 0-65535.");
        }
        this.requestedPort = port;
        this.auctionManager = java.util.Objects.requireNonNull(
                auctionManager, "auctionManager");
        this.accountHandler = java.util.Objects.requireNonNull(
                accountHandler, "accountHandler");
        this.dispatcher = new MessageDispatcher();
        dispatcher.register(NetworkMessage.LOGIN, accountHandler::handle);
        dispatcher.register(NetworkMessage.REGISTER, accountHandler::handle);
        dispatcher.register(NetworkMessage.AUCTION_LIST, this::handleAuctionList);
        dispatcher.register(NetworkMessage.PLACE_BID, this::handlePlaceBid);
        this.clientExecutor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "auction-client-handler");
            thread.setDaemon(true);
            return thread;
        });
        this.auctionManager.registerObserver(networkObserver);
    }

    /**
     * Khởi động accept loop không chặn luồng gọi.
     *
     * @throws IOException nếu không bind được cổng
     */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        ServerSocket socket = new ServerSocket(requestedPort);
        serverSocket = socket;
        running = true;
        acceptThread = new Thread(this::acceptLoop, "auction-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Trả về cổng thực tế sau khi server đã start.
     *
     * @return cổng hoặc -1 nếu server chưa chạy
     */
    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? -1 : socket.getLocalPort();
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(
                        socket, dispatcher, this::removeClient, this::addClient);
                clientExecutor.execute(handler);
            } catch (IOException exception) {
                if (running) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Không thể nhận kết nối client.", exception);
                }
            }
        }
    }

    private void addClient(ClientHandler handler) {
        clients.add(handler);
    }

    /**
     * Registers a route supplied by another job before the server starts.
     *
     * @param type request message type
     * @param handler route implementation
     */
    public synchronized void registerHandler(
            String type,
            Function<NetworkMessage, NetworkMessage> handler) {
        if (running) {
            throw new IllegalStateException("Không thể đăng ký route sau khi server đã chạy.");
        }
        dispatcher.register(type, handler);
    }

    /** Replaces a default route with the corresponding business handler before startup. */
    public synchronized void replaceHandler(
            String type,
            Function<NetworkMessage, NetworkMessage> handler) {
        if (running) {
            throw new IllegalStateException("Không thể thay route sau khi server đã chạy.");
        }
        dispatcher.replace(type, handler);
    }

    private NetworkMessage handleAuctionList(NetworkMessage request) {
        return new NetworkMessage(
                NetworkMessage.AUCTION_LIST,
                activeAuctionDtos(auctionManager));
    }

    private NetworkMessage handlePlaceBid(NetworkMessage message) {
        if (!(message.getPayload() instanceof BidRequestDTO request)) {
            return error("Payload PLACE_BID không hợp lệ.");
        }
        try {
            BidDTO result = auctionManager.processBid(request.auctionId(), request);
            return new NetworkMessage(NetworkMessage.BID_RESULT, result);
        } catch (InvalidBidException | AuctionClosedException
                 | IllegalArgumentException | IllegalStateException exception) {
            return error(exception.getMessage());
        }
    }

    private static NetworkMessage error(String message) {
        return new NetworkMessage(
                NetworkMessage.ERROR,
                message == null || message.isBlank() ? "Yêu cầu bị từ chối." : message);
    }

            private void removeClient(ClientHandler handler) {
                clients.remove(handler);
            }

            private void broadcast(NetworkMessage message) {
                for (ClientHandler client : clients) {
                    if (!client.send(message)) {
                        clients.remove(client);
                    }
                }
            }

            /**
             * Dừng server và đóng toàn bộ kết nối client.
             */
            @Override
            public synchronized void close() {
                if (!running && serverSocket == null) {
                    auctionManager.removeObserver(networkObserver);
                    clientExecutor.shutdownNow();
                    return;
                }
                running = false;
                auctionManager.removeObserver(networkObserver);
                ServerSocket socket = serverSocket;
                serverSocket = null;
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException exception) {
                        LOGGER.log(System.Logger.Level.DEBUG,
                                "Không thể đóng ServerSocket.", exception);
                    }
                }
                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
                clientExecutor.shutdownNow();
            }

            /**
             * Tạo payload danh sách DTO, không làm lộ model qua mạng.
             *
             * @return danh sách phiên đang hoạt động
             */
            static ArrayList<AuctionDTO> activeAuctionDtos(AuctionManager manager) {
                ArrayList<AuctionDTO> result = new ArrayList<>();
                for (Auction auction : manager.getActiveAuctions().values()) {
                    result.add(new AuctionDTO(
                    auction.getAuctionId(),
                    auction.getProduct().getName(),
                    auction.getCurrentPrice(),
                    auction.getStatus(),
                    auction.getEndsAt()));
        }
        return result;
    }
}
