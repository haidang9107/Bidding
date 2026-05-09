package org.example.model.user;

/**
 * Admin la duy nhat va chi tao duy nhat 1 lan khi db duoc khoi tao, Admin la user dau tien duoc them vao db, ID=1
 */
public class Admin extends User {
    private static Admin instance;

    // Default constructor for JSON
    public Admin() {
        super();
    }

    private Admin(Long id, String name, String nameAccount, String email, String password) {
        super(id, name, nameAccount, email, password);
    }

    public static Admin getInstance(Long id, String name, String nameAccount, String email, String password) {
        if (instance == null) {
            instance = new Admin(id, name, nameAccount, email, password);
        }
        return instance;
    }
}
