package org.example.server.network;

import org.example.model.user.User;
import org.example.payload.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.UserDao;
import org.example.server.service.user.auth.LogIn;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private static final ExecutorService logicPool = Executors.newVirtualThreadPerTaskExecutor();

    public CommandHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String message = inputLine.trim();
                if (!message.isEmpty()) {
                    logicPool.submit(() -> handleMessage(message));
                }
            }
        } catch (IOException e) {
            System.out.println(">>> Command Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            close();
        }
    }

    private void handleMessage(String message) {
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
            Connection conn = DatabaseManager.getConnection();
            UserDao userDao = new UserDao(conn);

            switch (request.getType()) {
                case LOGIN:
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
                    return new Response(MessageType.SUCCESS, true, "Đã nhận lệnh đặt thầu!", null);

                default:
                    return new Response(MessageType.ERROR, false, "Yêu cầu không hợp lệ", null);
            }
        } catch (SQLException e) {
            return new Response(MessageType.ERROR, false, "Database Error: " + e.getMessage(), null);
        }
    }

    private void sendMessage(String json) {
        out.println(json);
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
