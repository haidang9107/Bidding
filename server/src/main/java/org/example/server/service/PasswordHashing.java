package org.example.server.service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHashing {

	// 1. Hàm băm mật khẩu khi người dùng Đăng ký
	public static String hashPassword(String plainTextPassword) {
		// gensalt() tự động tạo một chuỗi muối ngẫu nhiên an toàn
		// Tham số 12 là độ phức tạp (work factor), mặc định là 10
		return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
	}

	// 2. Hàm kiểm tra mật khẩu khi người dùng Đăng nhập
	public static boolean checkPassword(String plainTextPassword, String hashedFromDB) {
		// Thuật toán tự tách Salt từ chuỗi hashedFromDB để đối chiếu
		return BCrypt.checkpw(plainTextPassword, hashedFromDB);
	}
}
