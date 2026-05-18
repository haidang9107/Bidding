package network;

import javafx.application.Platform;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý kết nối socket tới server.
 *
 * Áp dụng Singleton Pattern.
 *
 * Các controller có thể đăng ký listener bằng addListener(). Khi server gửi
 * message về, mọi listener đều được gọi (Observer pattern).
 *
 * Đặc biệt: callback được wrap trong Platform.runLater() để chạy trên JavaFX
 * thread, tránh lỗi "Not on FX application thread".
 */
public class SocketClient {

    private static final SocketClient INSTANCE = new SocketClient();

    public static SocketClient getInstance() {
        return INSTANCE;
    }

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;

    private final CopyOnWriteArrayList<ServerListener> listeners =
            new CopyOnWriteArrayList<>();

    private SocketClient() {
    }

    // =================================================================
    // Connection
    // =================================================================
    public synchronized void connect(String host, int port) throws IOException {
        if (connected) return;

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        // Thread riêng lắng nghe response từ server
        Thread listenerThread = new Thread(this::listenLoop, "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized void disconnect() {
        connected = false;
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        if (writer != null) writer.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }

    // =================================================================
    // Listener management
    // =================================================================
    public void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }

    // =================================================================
    // Send
    // =================================================================
    public synchronized void send(Request request) {
        if (!connected || writer == null) {
            System.err.println(">>> Chưa kết nối server, không gửi được");
            return;
        }
        String json = JsonConverter.toJson(request);
        writer.println(json);
    }

    // =================================================================
    // Receive loop
    // =================================================================
    private void listenLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Response response = JsonConverter.fromJson(line, Response.class);
                    dispatchToListeners(response);
                } catch (Exception e) {
                    System.err.println(">>> Lỗi parse response: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println(">>> Mất kết nối server: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }

    private void dispatchToListeners(Response response) {
        // Mọi callback đều chạy trên JavaFX thread cho an toàn UI
        Platform.runLater(() -> {
            for (ServerListener l : listeners) {
                try {
                    l.onMessage(response);
                } catch (Exception e) {
                    System.err.println(">>> Lỗi trong listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}
