package org.example.server.network;

import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles real-time notifications for a specific client.
 */
public class NotificationHandler {
    private final Socket socket;
    private final PrintWriter out;

    /**
     * Constructor for NotificationHandler.
     *
     * @param socket the client socket
     * @throws IOException if an I/O error occurs when creating the writer
     */
    public NotificationHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Sends a JSON message to the client.
     *
     * @param json the JSON string to send
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendMessage(String json) {
        out.println(json);
        return !out.checkError();
    }

    /**
     * Closes the notification socket and removes the client from the broadcaster.
     */
    public void close() {
        try {
            Broadcaster.removeClient(this);
            socket.close();
        } catch (IOException e) {
        }
    }
    
    /**
     * Checks if the socket is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return socket.isClosed();
    }
}
