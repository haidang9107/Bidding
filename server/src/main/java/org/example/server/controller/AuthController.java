package org.example.server.controller;

import org.example.model.user.User;
import org.example.model.enums.MessageType;
import org.example.payload.Response;
import org.example.server.service.user.auth.AuthService;

/**
 * Controller for handling authentication-related requests.
 */
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles login request.
     * Payload format: "username:password"
     */
    public Response<?> handleLogin(Object payload) {
        if (payload == null) {
            return new Response<>(MessageType.ERROR, false, "Missing login credentials", null);
        }

        String[] data = payload.toString().split(":");
        if (data.length < 2) {
            return new Response<>(MessageType.ERROR, false, "Invalid login format. Use 'username:password'", null);
        }

        String username = data[0];
        String password = data[1];

        User user = authService.authenticate(username, password);
        if (user != null) {
            return new Response<>(MessageType.SUCCESS, true, "Login successful", user);
        } else {
            return new Response<>(MessageType.ERROR, false, "Invalid username or password", null);
        }
    }

    /**
     * Handles signup request.
     * Payload format: "username:password:email:role"
     */
    public Response<?> handleSignup(Object payload) {
        if (payload == null) {
            return new Response<>(MessageType.ERROR, false, "Missing signup data", null);
        }

        String[] data = payload.toString().split(":");
        if (data.length < 4) {
            return new Response<>(MessageType.ERROR, false, "Invalid signup format. Use 'username:password:email:role'", null);
        }

        String username = data[0];
        String password = data[1];
        String email = data[2];
        String role = data[3];

        boolean success = authService.register(username, password, email, role);
        if (success) {
            return new Response<>(MessageType.SUCCESS, true, "Registration successful", null);
        } else {
            return new Response<>(MessageType.ERROR, false, "Registration failed (Username might already exist or internal error)", null);
        }
    }
}
