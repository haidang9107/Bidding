package org.example.server.network.command;

import org.example.dto.request.AdminUserControlRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for an administrator to ban a user from the system.
 */
public class AdminBanUserCommand implements Command {
    private final AdminController adminController;

    /**
     * Constructs an AdminBanUserCommand with the specified AdminController.
     *
     * @param adminController the controller for administrative operations
     */
    public AdminBanUserCommand(AdminController adminController) {
        this.adminController = adminController;
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
        return adminController.handleBanUser(adminReq);
    }
}
