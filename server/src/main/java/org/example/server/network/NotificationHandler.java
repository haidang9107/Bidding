package org.example.server.network;

import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class NotificationHandler {
    private final Socket socket;
    private final PrintWriter out;

    public NotificationHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String json) {
        out.println(json);
    }

    public void close() {
        try {
            Broadcaster.removeClient(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isClosed() {
        return socket.isClosed();
    }
}
