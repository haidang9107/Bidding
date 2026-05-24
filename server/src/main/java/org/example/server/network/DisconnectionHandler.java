package org.example.server.network;

import org.example.model.user.User;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.util.FileLogger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SOLID: Single Responsibility - Handles the business logic and resource cleanup of a client disconnection.
 * 
 * Lớp này đảm bảo khi một client ngắt kết nối (do lỗi mạng hoặc chủ động), 
 * toàn bộ tài nguyên liên quan (Socket, Session, Room, Buffer) sẽ được 
 * giải phóng triệt để để tránh rò rỉ bộ nhớ và lỗi trạng thái hệ thống.
 */
public class DisconnectionHandler {

    /**
     * Thực hiện quy trình dọn dẹp khi một client ngắt kết nối.
     * Quy trình bao gồm:
     * 1. Kiểm tra trạng thái User (có đang dẫn đầu phiên đấu giá nào không).
     * 2. Xóa client khỏi danh sách Broadcaster và RoomManager.
     * 3. Logout session của User.
     * 4. Đóng SocketChannel.
     * 
     * @param channel The socket channel that was disconnected.
     */
    public static void handle(SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) {
            cleanResources(channel);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            ProductDao productDao = new ProductDao();
            boolean isLeading = productDao.isUserLeadingAnyAuction(conn, user.getAccountname());

            if (isLeading) {
                FileLogger.warn("User " + user.getAccountname() + " disconnected while leading an auction. Session removed, but bid remains active.");
            } else {
                FileLogger.info("User " + user.getAccountname() + " disconnected safely.");
            }

        } catch (SQLException e) {
            FileLogger.error("Error checking auction status during disconnection for user: " + user.getAccountname(), e);
        } finally {
            cleanResources(channel);
        }
    }

    private static void cleanResources(SocketChannel channel) {
        try {
            Broadcaster.removeClient(channel);
            RoomManager.removeChannel(channel);
            SessionManager.logout(channel);
            HeartbeatRegistry.remove(channel);
            
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            FileLogger.error("Error closing channel during cleanup", e);
        }
    }
}
