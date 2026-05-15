
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    /**
     * Connects to the server.
     */
    public void connect(String host, int port) throws IOException {
        if (connected) return;

        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.connected = true;

        System.out.println(">>> Connected to server at " + host + ":" + port);

        // Start a thread to listen for messages from server
        startListening();
    }

    /**
     * Sends a request to the server.
     */
    public void sendRequest(Request request) {
        if (!connected) {
            System.err.println(">>> Not connected to server!");
            return;
        }
        String json = JsonConverter.toJson(request);
        out.println(json);
    }

    /**
     * Continuously listens for responses from the server in a separate thread.
     */
    private void startListening() {
        new Thread(() -> {
            try {
                String responseLine;
                while (connected && (responseLine = in.readLine()) != null) {
                    System.out.println(">>> Received from server: " + responseLine);
                    
                    // Convert JSON back to Response object
                    Response response = JsonConverter.fromJson(responseLine, Response.class);
                    
                    // Handle response (this is where you'd update your UI controllers later)
                    handleResponse(response);
                }
            } catch (IOException e) {
                System.err.println(">>> Disconnected from server: " + e.getMessage());
                connected = false;
            }
        }).start();
    }

    private void handleResponse(Response response) {
        // Placeholder: later we will use a callback or event system to notify JavaFX
        System.out.println(">>> Processed response: " + response.getMessage());
    }

    public void disconnect() {
        try {
            connected = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
