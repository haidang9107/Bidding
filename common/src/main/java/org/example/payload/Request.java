package org.example.payload;

/**
 * Represents a request from a client to the server.
 */
public class Request {
    private MessageType type;
    private Object payload;
    private String token; // Optional: for session management

    /**
     * Default constructor for Request.
     */
    public Request() {}

    /**
     * Constructor for Request with type and payload.
     *
     * @param type the type of the message
     * @param payload the payload of the request
     */
    public Request(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * Gets the message type.
     *
     * @return the type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Sets the message type.
     *
     * @param type the type to set
     */
    public void setType(MessageType type) {
        this.type = type;
    }

    /**
     * Gets the request payload.
     *
     * @return the payload
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Sets the request payload.
     *
     * @param payload the payload to set
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    /**
     * Gets the session token.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the session token.
     *
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }
}
