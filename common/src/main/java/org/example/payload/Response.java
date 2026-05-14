package org.example.payload;

/**
 * Represents a response from the server to a client.
 *
 * @param <T> the type of data contained in the response
 */
public class Response<T> {
    private MessageType type;
    private boolean success;
    private String message;
    private T data;

    /**
     * Default constructor for Response.
     */
    public Response() {}

    /**
     * Constructor for Response with all fields.
     *
     * @param type the type of the message
     * @param success whether the operation was successful
     * @param message a descriptive message
     * @param data the response data
     */
    public Response(MessageType type, boolean success, String message, T data) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.data = data;
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
     * Checks if the operation was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the operation.
     *
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the descriptive message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the descriptive message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the response data.
     *
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * Sets the response data.
     *
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }
}
