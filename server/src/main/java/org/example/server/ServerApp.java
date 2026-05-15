package org.example.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServerApp {
    // Thông tin kết nối MySQL - Chỉnh sửa theo đúng thông tin bạn vừa thiết lập trong IntelliJ
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bidding";
    private static final String DB_USER = "MINHANH";
    private static final String DB_PASS = "123456";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Đổi endpoint thành /api/products để phù hợp với database đấu giá
        server.createContext("/api/products", new ProductHandler());

        server.setExecutor(null);
        System.out.println("Server đấu giá đã chạy tại cổng: " + port);
        server.start();
    }

    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder jsonResponse = new StringBuilder("[");

            // Kết nối database và lấy dữ liệu
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT product_id, product_name, starting_price FROM products")) {

                while (rs.next()) {
                    if (jsonResponse.length() > 1) jsonResponse.append(",");
                    jsonResponse.append(String.format(
                            "{\"id\":\"%s\", \"name\":\"%s\", \"price\":%f}",
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getDouble("starting_price")
                    ));
                }
                jsonResponse.append("]");

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                sendResponse(exchange, jsonResponse.toString(), 200);

            } catch (SQLException e) {
                e.printStackTrace();
                sendResponse(exchange, "Database Error: " + e.getMessage(), 500);
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        // Fix lỗi font tiếng Việt bằng cách dùng byte UTF-8
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}