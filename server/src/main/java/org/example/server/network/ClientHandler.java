package org.example.server.network;

import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.payload.MessageType;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.server.service.user.auth.LogIn;
import org.example.server.service.user.auth.SignUp;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final SocketChannel socketChannel;
    private final NioWorker worker;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final StringBuilder inputBuffer = new StringBuilder();

    // Logic Thread Pool (Business Pool)
    private static final ExecutorService logicPool = Executors.newVirtualThreadPerTaskExecutor();

    public ClientHandler(SocketChannel socketChannel, NioWorker worker) {
        this.socketChannel = socketChannel;
        this.worker = worker;
    }

    public void read() throws IOException {
        int bytesRead = socketChannel.read(readBuffer);
        if (bytesRead == -1) {
            throw new IOException("Client closed connection");
        }

        readBuffer.flip();
        String received = StandardCharsets.UTF_8.decode(readBuffer).toString();
        inputBuffer.append(received);
        readBuffer.clear();

        processInput();
    }

    private void processInput() {
        int newlineIndex;
        while ((newlineIndex = inputBuffer.indexOf("\n")) != -1) {
            String message = inputBuffer.substring(0, newlineIndex).trim();
            inputBuffer.delete(0, newlineIndex + 1);

            if (!message.isEmpty()) {
                logicPool.submit(() -> handleMessage(message));
            }
        }
    }

    private void handleMessage(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] >>> Handling message: " + message);
        try {
            Request request = JsonConverter.fromJson(message, Request.class);
            Response response = handleRequest(request);
            sendMessage(JsonConverter.toJson(response));
        } catch (Exception e) {
            System.err.println(">>> Error handling message: " + e.getMessage());
            sendMessage(JsonConverter.toJson(new Response(MessageType.ERROR, false, "Internal Server Error", null)));
        }
    }

    private Response handleRequest(Request request) {
        try {
            // Khởi tạo DAO và Service ngay trong luồng Logic
            Connection conn = DatabaseManager.getConnection();
            UserDao userDao = new UserDao(conn);

            switch (request.getType()) {
                case LOGIN:
                    // Giả sử payload là "username:password" cho demo nhanh
                    String[] loginData = request.getPayload().toString().split(":");
                    if (loginData.length < 2) return new Response(MessageType.ERROR, false, "Invalid payload format", null);
                    
                    LogIn loginService = new LogIn(userDao);
                    User user = loginService.authenticate(loginData[0], loginData[1]);
                    
                    if (user != null) {
                        return new Response(MessageType.SUCCESS, true, "Chào mừng " + user.getUsername(), user);
                    } else {
                        return new Response(MessageType.ERROR, false, "Sai tài khoản hoặc mật khẩu", null);
                    }

                case BID_PLACE:
                    // Sẽ xử lý logic đấu giá ở đây
                    return new Response(MessageType.SUCCESS, true, "Đã nhận lệnh đặt thầu!", null);

                default:
                    return new Response(MessageType.ERROR, false, "Yêu cầu không hợp lệ", null);
            }
        } catch (SQLException e) {
            return new Response(MessageType.ERROR, false, "Database Error: " + e.getMessage(), null);
        }
    }

    public void sendMessage(String message) {
        try {
            String framedMessage = message + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(framedMessage.getBytes(StandardCharsets.UTF_8));
            synchronized (socketChannel) {
                while (buffer.hasRemaining()) {
                    socketChannel.write(buffer);
                }
            }
        } catch (IOException e) {
            System.err.println(">>> Error sending message: " + e.getMessage());
        }
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return socketChannel.getRemoteAddress();
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
