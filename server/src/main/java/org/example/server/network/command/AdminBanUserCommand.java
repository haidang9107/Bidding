package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.model.enums.MessageType;
import org.example.dto.request.AdminUserControlRequest;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.server.network.DisconnectionHandler;
import org.example.server.network.SessionManager;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Command for an administrator to ban a user from the system.
 */
@RequiresRole(UserRole.ADMIN)
public class AdminBanUserCommand implements Command {
    private final AdminController adminController;
    private final DisconnectionHandler disconnectionHandler;

    /**
     * Constructs an AdminBanUserCommand with the specified AdminController.
     *
     * @param adminController the controller for administrative operations
     * @param disconnectionHandler the handler for client disconnections
     */
    public AdminBanUserCommand(AdminController adminController, DisconnectionHandler disconnectionHandler) {
        this.adminController = adminController;
        this.disconnectionHandler = disconnectionHandler;
    }

    /**
     * Executes the ban user command.
     *
     * @param request the request containing AdminUserControlRequest
     * @param channel the socket channel of the administrator
     * @return the response from the admin controller
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        AdminUserControlRequest adminReq = JsonConverter.convert(request.getPayload(), AdminUserControlRequest.class);
        
        if (adminReq != null) {
            User currentUser = SessionManager.getUser(channel);
            if (currentUser != null && currentUser.getAccountname().equals(adminReq.getTargetAccountname())) {
                return new Response<>(MessageType.ERROR, false, "Bạn không thể tự khóa tài khoản của chính mình!", null);
            }
        }

        Response<?> response = adminController.handleBanUser(adminReq);

        // If user was successfully banned (status = 1), kick them out immediately
        if (response.isSuccess() && adminReq != null && adminReq.getStatus() == 1) {
            String targetAccount = adminReq.getTargetAccountname();
            SocketChannel userChannel = SessionManager.findChannelByUsername(targetAccount);
            if (userChannel != null) {
                FileLogger.info("Admin banned user " + targetAccount + ". Kicking out active session.");
                notifyBanned(userChannel);
                disconnectionHandler.handle(userChannel);
            }
        }

        return response;
    }

    private void notifyBanned(SocketChannel channel) {
        try {
            Response<String> response = new Response<>(MessageType.ERROR, false,
                    "Your account has been BANNED by the administrator. You have been disconnected.", "ACCOUNT_BANNED");
            String json = JsonConverter.toJson(response) + "\n";
            channel.write(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            FileLogger.warn("Failed to notify ban to channel: " + channel);
        }
    }
}
