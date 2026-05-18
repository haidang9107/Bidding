package session;

import org.example.model.user.User;

/**
 * Lưu thông tin phiên đăng nhập hiện tại của client.
 * Singleton: dùng chung cho tất cả controller.
 */
public final class Session {

    private static final Session INSTANCE = new Session();

    private User currentUser;

    private Session() {
    }

    public static Session getInstance() {
        return INSTANCE;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }
}
