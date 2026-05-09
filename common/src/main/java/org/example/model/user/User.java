package org.example.model.user;

abstract class User {
	private String name;
	private String email;
	private String password;
	private  String nameAccount;

	User(String name, String nameAccount, String email, String password) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.nameAccount = nameAccount;
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

	public String getPassword() {
		return password;
	}
	public  void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}
}
