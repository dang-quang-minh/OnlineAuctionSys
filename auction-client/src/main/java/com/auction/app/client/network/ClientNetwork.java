
package com.auction.app.client.network;

import com.auction.app.common.dto.NetworkMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Cổng giao tiếp mạng phía Client sử dụng Java Socket.
 * Quản lý việc gửi và nhận NetworkMessage.
 */
public class ClientNetwork {

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread listenerThread;

    private volatile boolean running;

    private volatile MessageListener messageListener;
    private volatile Consumer<String> disconnectListener;

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setDisconnectListener(Consumer<String> disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    /**
     * Kết nối tới Server và khởi chạy luồng nhận dữ liệu.
     */
    public synchronized void connect(String host, int port)
            throws IOException {

        if (isConnected()) {
            return;
        }

        // Dọn dẹp kết nối cũ nếu có.
        disconnect();

        Socket newSocket = new Socket();

        try {
            newSocket.connect(
                    new java.net.InetSocketAddress(host, port)
            );

            ObjectOutputStream newOutput =
                    new ObjectOutputStream(newSocket.getOutputStream());

            newOutput.flush();

            ObjectInputStream newInput =
                    new ObjectInputStream(newSocket.getInputStream());

            // Chỉ gán vào đối tượng sau khi khởi tạo thành công.
            this.socket = newSocket;
            this.output = newOutput;
            this.input = newInput;

            this.running = true;

            this.listenerThread = new Thread(
                    this::listenLoop,
                    "Client-Network-Listener"
            );

            this.listenerThread.setDaemon(true);
            this.listenerThread.start();

        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }

            throw e;
        }
    }

    /**
     * Gửi NetworkMessage tới Server.
     */
    public synchronized void sendMessage(NetworkMessage msg)
            throws IOException {

        if (msg == null) {
            throw new IllegalArgumentException(
                    "NetworkMessage cannot be null"
            );
        }

        if (!isConnected() || output == null) {
            throw new IOException(
                    "Client is not connected to the server"
            );
        }

        output.writeObject(msg);
        output.flush();
        output.reset();
    }

    /**
     * Vòng lặp nhận dữ liệu chạy trên luồng nền.
     */
    private void listenLoop() {

        ObjectInputStream currentInput = input;

        if (currentInput == null) {
            return;
        }

        while (running && isConnected()) {
            try {
                Object received = currentInput.readObject();

                if (!(received instanceof NetworkMessage message)) {
                    throw new IOException(
                            "Received an invalid message from server"
                    );
                }

                MessageListener listener = messageListener;

                if (listener != null) {
                    listener.onMessageReceived(message);
                }

            } catch (EOFException | SocketException e) {
                if (running) {
                    handleUnexpectedDisconnect(
                            "Mất kết nối tới máy chủ."
                    );
                }
                break;

            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    handleUnexpectedDisconnect(
                            "Lỗi đọc dữ liệu từ máy chủ: "
                                    + e.getMessage()
                    );
                }
                break;
            }
        }
    }

    /**
     * Xử lý khi kết nối bị mất ngoài ý muốn.
     */
    private void handleUnexpectedDisconnect(String reason) {
        disconnect();

        Consumer<String> listener = disconnectListener;

        if (listener != null) {
            listener.accept(reason);
        }
    }

    /**
     * Đóng Socket và giải phóng tài nguyên.
     */
    public synchronized void disconnect() {
        running = false;

        Socket oldSocket = socket;
        ObjectInputStream oldInput = input;
        ObjectOutputStream oldOutput = output;

        // Xóa tham chiếu để tránh sử dụng nhầm kết nối cũ.
        socket = null;
        input = null;
        output = null;
        listenerThread = null;

        // Đóng Socket trước để giải phóng readObject() đang chờ.
        if (oldSocket != null && !oldSocket.isClosed()) {
            try {
                oldSocket.close();
            } catch (IOException ignored) {
            }
        }

        if (oldInput != null) {
            try {
                oldInput.close();
            } catch (IOException ignored) {
            }
        }

        if (oldOutput != null) {
            try {
                oldOutput.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Kiểm tra trạng thái kết nối.
     */
    public boolean isConnected() {
        return running
                && socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }
}
