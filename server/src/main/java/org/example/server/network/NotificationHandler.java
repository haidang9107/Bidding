package org.example.server.network;

import org.example.util.FileLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Handles real-time notifications for a specific client using NIO SocketChannel.
 */
public class NotificationHandler {
    private final SocketChannel clientChannel;

    public NotificationHandler(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    /**
     * Sends a JSON message to the client.
     *
     * @param json the JSON string to send
     * @return true if the message was sent successfully, false otherwise
     */
    public synchronized boolean sendMessage(String json) {
        try {
            String message = json + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                int written = clientChannel.write(buffer);
                if (written == 0) {
                    Thread.onSpinWait(); // Avoid 100% CPU spin
                }
            }
            return true;
        } catch (IOException e) {
            FileLogger.error("Failed to send notification to client", e);
            return false;
        }
    }

    /**
     * Closes the notification socket and removes the client from the broadcaster.
     */
    public void close() {
        try {
            Broadcaster.removeClient(this);
            clientChannel.close();
        } catch (IOException e) {
            FileLogger.error("Error closing notification channel", e);
        }
    }
    
    /**
     * Checks if the socket is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return !clientChannel.isOpen();
    }
}
