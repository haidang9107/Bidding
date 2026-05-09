package org.example.payload;

public class Response {
    private MessageType type;
    private boolean success;
    private String message;
    private Object data;

    public Response() {}

    public Response(MessageType type, boolean success, String message, Object data) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
