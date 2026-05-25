package org.example.server.network.command;

import org.example.dto.request.PaginationRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AdminController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for an administrator to retrieve a paginated list of all users.
 */
public class AdminGetAllUsersCommand implements Command {
    private final AdminController adminController;

    /**
     * Constructs an AdminGetAllUsersCommand with the specified AdminController.
     *
     * @param adminController the controller for administrative operations
     */
    public AdminGetAllUsersCommand(AdminController adminController) {
        this.adminController = adminController;
    }

    /**
     * Executes the get all users command.
     *
     * @param request the request containing PaginationRequest
     * @param channel the socket channel of the administrator
     * @return the response containing the list of users
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        PaginationRequest pagReq = JsonConverter.convert(request.getPayload(), PaginationRequest.class);
        return adminController.handleGetAllUsers(pagReq);
    }
}
