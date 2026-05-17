package org.example.model.user;

import org.example.model.enums.Gender;
import org.example.model.enums.UserRole;
import org.example.model.product.Item;
import java.sql.Timestamp;

/**
 * Represents a regular member who can both bid and sell.
 */
public class Member extends User implements IBidder, ISeller {

    public Member() {
        super();
        this.setRole(UserRole.MEMBER);
    }

    public Member(int userId, String username, String password, String email, 
                  String phoneNumber, Gender gender, String avt, long balance, 
                  long blockedBalance, Timestamp createdAt) {
        super(userId, username, password, email, phoneNumber, gender, avt, 
              balance, blockedBalance, UserRole.MEMBER, createdAt);
    }

    @Override
    public void placeBid(int productId, long amount) {
        // Logic will be implemented in controller/service
    }

    @Override
    public void createAuction(Item item) {
        // Logic will be implemented in controller/service
    }

    @Override
    public void cancelAuction(int productId) {
        // Logic will be implemented in controller/service
    }
}
