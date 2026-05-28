package org.example.server.network.command;

import org.example.server.annotation.RequiresRole;
import org.example.model.enums.UserRole;
import org.example.model.enums.MessageType;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;

import java.nio.channels.SocketChannel;

/**
 * Command for an administrator to retrieve system-wide statistics.
 */
@RequiresRole(UserRole.ADMIN)
public class AdminGetStatsCommand implements Command {
    private final AdminController adminController;

    public AdminGetStatsCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        return adminController.handleGetStats();
    }
}
