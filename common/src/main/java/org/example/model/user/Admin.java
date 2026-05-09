package org.example.model.user;

public class Admin extends User {
	private static Admin admin;

	private Admin(String name, String nameAccount, String email, String password) {
		super(name, nameAccount, email, password);
	}
}
