package com.auction.app.client.network;

import com.auction.app.common.dto.AuctionDTO;
import com.auction.app.common.dto.BidDTO;
import com.auction.app.common.dto.BidRequestDTO;
import com.auction.app.common.dto.LoginResultDTO;
import com.auction.app.common.dto.RegisterRequestDTO;
import com.auction.app.common.dto.RegisterResultDTO;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Asynchronous client-side API for the server operations used by the UI.
 * Network I/O runs away from the JavaFX application thread.
 */
public final class AuctionGateway {

    private static final AuctionGateway INSTANCE = new AuctionGateway(
            ClientSession.getInstance(),
            Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "auction-gateway");
                thread.setDaemon(true);
                return thread;
            }));

    private final ClientSession session;
    private final ExecutorService executor;
    private CompletableFuture<Void> requestTail = CompletableFuture.completedFuture(null);

    private AuctionGateway(ClientSession session, ExecutorService executor) {
        this.session = Objects.requireNonNull(session, "session");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public static AuctionGateway getInstance() {
        return INSTANCE;
    }

    public CompletionStage<LoginResultDTO> login(String username, String password) {
        return enqueue(result -> session.login(
                username,
                password,
                result::complete,
                message -> completeWithError(result, message)));
    }

    public CompletionStage<RegisterResultDTO> register(RegisterRequestDTO request) {
        Objects.requireNonNull(request, "request");
        return enqueue(result -> session.register(
                request,
                result::complete,
                message -> completeWithError(result, message)));
    }

    public CompletionStage<List<AuctionDTO>> requestAuctions() {
        return enqueue(result -> session.requestAuctions(
                result::complete,
                message -> completeWithError(result, message)));
    }

    public CompletionStage<BidDTO> placeBid(BidRequestDTO request) {
        Objects.requireNonNull(request, "request");
        return enqueue(result -> session.placeBidWithResult(
                request.auctionId(),
                request.bidderId(),
                request.amount(),
                result::complete,
                message -> completeWithError(result, message)));
    }

    public void setNewBidListener(Consumer<BidDTO> listener) {
        session.setBidConsumer(listener);
    }

    public String getCurrentUserId() {
        return session.getCurrentUserId();
    }

    public String getCurrentUserRole() {
        return session.getCurrentUserRole();
    }

    public String getCurrentUsername() {
        return session.getCurrentUsername();
    }

    public void disconnect() {
        session.disconnect();
    }

    private synchronized <T> CompletionStage<T> enqueue(Request<T> request) {
        CompletableFuture<T> response = new CompletableFuture<>();

        requestTail = requestTail.handle((ignored, previousFailure) -> (Void) null)
                .thenComposeAsync(ignored -> {
                    try {
                        request.send(response);
                    } catch (IOException | RuntimeException exception) {
                        response.completeExceptionally(exception);
                    }
                    return response.handle((value, failure) -> (Void) null);
                }, executor);

        return response.minimalCompletionStage();
    }

    private static void completeWithError(
            CompletableFuture<?> result,
            String message) {
        String errorMessage = message == null || message.isBlank()
                ? "Yêu cầu tới máy chủ thất bại."
                : message;
        result.completeExceptionally(new IOException(errorMessage));
    }

    @FunctionalInterface
    private interface Request<T> {
        void send(CompletableFuture<T> response) throws IOException;
    }
}
