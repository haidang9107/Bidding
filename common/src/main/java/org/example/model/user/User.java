package org.example.model.user;

public abstract class User {
    //id la index trong db
    private Long id;
    private String name;
    private String email;
    private String password;
    private String nameAccount;

    // Default constructor for JSON deserialization
    public User() {
    }

    public User(Long id, String name, String nameAccount, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.nameAccount = nameAccount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameAccount() {
        return nameAccount;
    }

    public void setNameAccount(String nameAccount) {
        this.nameAccount = nameAccount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
