package org.example.client.network;

import javafx.application.Platform;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.util.JsonConverter;

import java.io.*;
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

    /** Interval between PING messages sent to the server. 25s is well under
     *  any typical server-side idle timeout (~60s) but slow enough not to
     *  spam logs. */
    private static final long PING_INTERVAL_MS = 25_000L;

    private static final SocketClient INSTANCE = new SocketClient();

    public static SocketClient getInstance() {
        return INSTANCE;
    }

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;
    private Thread heartbeatThread;

    // Ghi nhớ host/port đã connect để có thể tự kết nối lại sau khi bị
    // server đóng socket (vd bị ban / đăng nhập nơi khác).
    private String lastHost;
    private int lastPort;

    private final CopyOnWriteArrayList<ServerListener> listeners =
            new CopyOnWriteArrayList<>();

    private SocketClient() {
    }

    // =================================================================
    // Connection
    // =================================================================
    public synchronized void connect(String host, int port) throws IOException {
        if (connected) return;
        this.lastHost = host;
        this.lastPort = port;

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

        // Heartbeat thread — sends PING every 25 seconds so the server's
        // HeartbeatRegistry doesn't time us out as idle. The PONG response
        // arrives on the listener thread like any other Response; we don't
        // need any specific handling beyond keeping the socket warm. Daemon
        // so JVM can exit cleanly.
        heartbeatThread = new Thread(this::heartbeatLoop, "client-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Kết nối lại tới máy chủ đã connect lần gần nhất. Dùng sau khi socket bị
     * đóng (bị ban / đăng nhập nơi khác) để người dùng có thể đăng nhập lại.
     */
    public synchronized void reconnect() throws IOException {
        if (connected) return;
        if (lastHost == null) throw new IOException("Chưa từng kết nối trước đó");
        connect(lastHost, lastPort);
    }

    /**
     * Closes the socket and stops the listener thread. Called from
     * {@link ClientApp#start} on a background thread so the FX thread can
     * exit immediately. Order matters: setting connected=false first stops
     * the listener loop from processing more lines, then closing the socket
     * itself unblocks any read that was waiting on the input stream.
     */
    public synchronized void disconnect() {
        connected = false;
        // Close the underlying socket FIRST — this unblocks the listener
        // thread that's stuck inside reader.readLine(). Closing reader/writer
        // alone wouldn't necessarily interrupt the blocking read on the
        // socket on every JDK.
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        if (writer != null) writer.close();
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

    /**
     * Heartbeat loop — sends {@code MessageType.PING} every PING_INTERVAL_MS
     * so the server's HeartbeatRegistry keeps our session alive. The server
     * answers with PONG which arrives on the listener loop; listeners can
     * ignore it. If sending fails we set {@code connected = false} so the
     * loop exits cleanly.
     */
    private void heartbeatLoop() {
        while (connected) {
            try {
                Thread.sleep(PING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!connected) return;
            try {
                send(new Request(org.example.model.enums.MessageType.PING, null));
            } catch (Exception e) {
                // Probably the socket got closed under us — just exit, the
                // listener loop will report the disconnect.
                return;
            }
        }
    }

    private void dispatchToListeners(Response response) {
        // Mọi callback đều chạy trên JavaFX thread cho an toàn UI
        Platform.runLater(() -> {
            // --- Xử lý tập trung khi server đẩy client ra (ban / đăng nhập nơi khác) ---
            // Server gửi Response(ERROR, false, "<lý do>", data="KICKED_OUT" hoặc
            // "ACCOUNT_BANNED") rồi đóng kết nối. Bất kể đang ở màn hình nào,
            // client phải đăng xuất và quay về màn đăng nhập, không để treo.
            if (response != null
                    && response.getType() == org.example.model.enums.MessageType.ERROR
                    && response.getData() != null) {
                String code = String.valueOf(response.getData());
                if ("KICKED_OUT".equals(code) || "ACCOUNT_BANNED".equals(code)) {
                    handleForcedLogout(response.getMessage(), code);
                    return; // không phát tiếp cho listener khác
                }
            }

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

    /**
     * Đăng xuất bắt buộc do server khởi xướng (bị ban hoặc bị đăng nhập từ
     * thiết bị khác). Xóa session, dọn listener, hiện thông báo rồi đưa người
     * dùng về màn đăng nhập. Gọi trên JavaFX thread.
     */
    private void handleForcedLogout(String message, String code) {
        try {
            org.example.client.session.Session.getInstance().logout();
        } catch (Exception ignored) {}
        // Gỡ mọi listener của màn hiện tại để chúng không xử lý gì thêm.
        listeners.clear();

        String title = "ACCOUNT_BANNED".equals(code)
                ? "Tài khoản bị khóa" : "Phiên đăng nhập kết thúc";
        String body = (message == null || message.isEmpty())
                ? ("ACCOUNT_BANNED".equals(code)
                    ? "Tài khoản của bạn đã bị quản trị viên khóa."
                    : "Tài khoản của bạn vừa đăng nhập ở nơi khác.")
                : message;
        try {
            org.example.client.notification.NotificationService.getInstance()
                    .error(title, body);
        } catch (Exception ignored) {}

        // Quay về màn đăng nhập.
        try {
            org.example.client.util.SceneRouter.go("/view/Login.fxml", "Đăng nhập");
        } catch (Exception e) {
            System.err.println(">>> Không thể quay về màn đăng nhập: " + e.getMessage());
        }
    }
}
