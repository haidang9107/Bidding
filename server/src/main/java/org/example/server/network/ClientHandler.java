package org.example.server.network;

import org.example.payload.Request;
import org.example.payload.Response;
import org.example.payload.MessageType;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Set up communication streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            // Keep listening for messages from this specific client
            while ((inputLine = in.readLine()) != null) {
                System.out.println(">>> Received from client: " + inputLine);

                // 1. Convert JSON string to Request object
                Request request = JsonConverter.fromJson(inputLine, Request.class);

                // 2. Handle the request based on its type
                Response response = handleRequest(request);

                // 3. Send response back to client as JSON
                out.println(JsonConverter.toJson(response));
            }
        } catch (IOException e) {
            System.out.println(">>> Client disconnected: " + socket.getInetAddress());
        } finally {
            closeConnection();
        }
    }

    private Response handleRequest(Request request) {
        // This is a simple placeholder logic. 
        // Later, this will call AuctionService to process bids.
        switch (request.getType()) {
            case LOGIN:
                return new Response(MessageType.SUCCESS, true, "Login successful!", null);
            case BID_PLACE:
                return new Response(MessageType.SUCCESS, true, "Bid placed successfully!", request.getPayload());
            default:
                return new Response(MessageType.ERROR, false, "Unknown request type", null);
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
