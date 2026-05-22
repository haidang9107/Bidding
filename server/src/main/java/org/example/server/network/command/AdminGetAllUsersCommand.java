package org.example.server.network.command;

import org.example.dto.PaginationRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

public class AdminGetAllUsersCommand implements Command {
    private final AdminController adminController;

    public AdminGetAllUsersCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        PaginationRequest pagReq = JsonConverter.convert(request.getPayload(), PaginationRequest.class);
        return adminController.handleGetAllUsers(pagReq);
    }
}
