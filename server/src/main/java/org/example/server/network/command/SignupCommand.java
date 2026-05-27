package org.example.server.network.command;

import org.example.model.enums.MessageType;
import org.example.dto.request.SignupRequest;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.controller.AuthController;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command to handle new user registration.
 */
public class SignupCommand implements Command {
    private final AuthController authController;

    /**
     * Constructs a SignupCommand with the specified AuthController.
     *
     * @param authController the controller for authentication operations
     */
    public SignupCommand(AuthController authController) {
        this.authController = authController;
    }

    /**
     * Executes the signup command.
     *
     * @param request the request containing SignupRequest
     * @param channel the socket channel of the user
     * @return the response indicating success or failure of the registration
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        SignupRequest signupReq = JsonConverter.convert(request.getPayload(), SignupRequest.class);
        return authController.handleSignup(signupReq);
    }
}
