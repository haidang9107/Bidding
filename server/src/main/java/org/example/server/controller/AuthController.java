package org.example.server.controller;

import org.example.dto.request.LoginRequest;
import org.example.dto.request.SignupRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
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
     * Authenticates and returns the User object.
     * Throws exception if authentication fails.
     */
    public User authenticateAndGetUser(LoginRequest loginReq) {
        if (loginReq == null || loginReq.getUsername() == null) {
            return null; // Will be handled by the Command if needed
        }
        return authService.authenticate(loginReq.getUsername(), loginReq.getPassword());
    }

    /**
     * Handles signup request.
     */
    public Response<?> handleSignup(SignupRequest signupReq) {
        if (signupReq == null || signupReq.getUsername() == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid signup data", null);
        }

        authService.register(signupReq.getUsername(), signupReq.getPassword(), signupReq.getEmail());
        return new Response<>(MessageType.SUCCESS, true, "Registration successful", null);
    }
}
