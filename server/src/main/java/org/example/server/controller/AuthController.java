package org.example.server.controller;

import org.example.dto.LoginRequest;
import org.example.dto.SignupRequest;
import org.example.dto.UserResponse;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Response;
import org.example.server.exception.AuthException;
import org.example.server.service.user.auth.AuthService;
import org.example.util.JsonConverter;

/**
 * Controller for handling authentication-related requests.
 */
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates and returns the raw User object.
     * Used by LoginCommand for session management.
     */
    public Object authenticateAndGetUser(LoginRequest loginReq) {
        if (loginReq == null || loginReq.getUsername() == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid login credentials", null);
        }

        try {
            return authService.authenticate(loginReq.getUsername(), loginReq.getPassword());
        } catch (AuthException e) {
            return new Response<>(MessageType.ERROR, false, e.getMessage(), null);
        }
    }

    /**
     * Handles signup request.
     */
    public Response<?> handleSignup(SignupRequest signupReq) {
        if (signupReq == null || signupReq.getUsername() == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid signup data", null);
        }

        // All new users are registered as MEMBER by default for security.
        boolean success = authService.register(signupReq.getUsername(), signupReq.getPassword(), 
                                             signupReq.getEmail());
        if (success) {
            return new Response<>(MessageType.SUCCESS, true, "Registration successful as MEMBER", null);
        } else {
            return new Response<>(MessageType.ERROR, false, "Registration failed (Username might exist)", null);
        }
    }
}
