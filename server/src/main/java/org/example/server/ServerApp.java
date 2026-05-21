package org.example.server;

import org.example.server.controller.AuthController;
import org.example.server.controller.AdminController;
import org.example.server.controller.UserController;
import org.example.server.controller.BidController;
import org.example.server.controller.ProductController;
import org.example.server.controller.FinanceController;
import org.example.server.network.AuctionMonitor;
import org.example.server.network.SocketServer;
import org.example.server.network.command.*;
import org.example.server.service.bid.BidService;
import org.example.server.service.product.ProductService;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.model.enums.MessageType;
import org.example.util.FileLogger;

/**
 * Main Entry point for the Bidding Server.
 * Initializes all dependencies and starts the Socket Server.
 */
public class ServerApp {

    public static void main(String[] args) {
        FileLogger.info("Starting Bidding Server...");

        // 1. Initialize Services
        AuthService authService = new AuthService();
        ProductService productService = new ProductService();
        BidService bidService = new BidService();
        DepositService depositService = new DepositService();
        WithdrawService withdrawService = new WithdrawService();
        TransferService transferService = new TransferService();

        // 2. Initialize Controllers
        AuthController authController = new AuthController(authService);
        ProductController productController = new ProductController(productService);
        BidController bidController = new BidController(bidService);
        AdminController adminController = new AdminController();
        UserController userController = new UserController();
        FinanceController financeController = new FinanceController(depositService, withdrawService, transferService);

        // 3. Initialize Command Registry
        CommandRegistry registry = new CommandRegistry();
        registry.register(MessageType.LOGIN, new LoginCommand(authController));
        registry.register(MessageType.SIGNUP, new SignupCommand(authController));
        registry.register(MessageType.LOGOUT, new LogoutCommand());
        registry.register(MessageType.PING, new PingCommand());
        registry.register(MessageType.PRODUCT_LIST, new ProductListCommand(productController));
        registry.register(MessageType.PRODUCT_DETAIL, new ProductDetailCommand(productController));
        registry.register(MessageType.PRODUCT_ADD, new ProductAddCommand(productController));
        registry.register(MessageType.BID_PLACE, new BidPlaceCommand(bidController));
        registry.register(MessageType.AUTO_BID_SET, new AutoBidSetCommand(bidController));
        registry.register(MessageType.AUTO_BID_CANCEL, new AutoBidCancelCommand(bidController));
        registry.register(MessageType.JOIN_AUCTION_ROOM, new JoinAuctionRoomCommand());
        registry.register(MessageType.LEAVE_AUCTION_ROOM, new LeaveAuctionRoomCommand());
        
        // User & Admin Commands
        registry.register(MessageType.GET_PROFILE, new GetProfileCommand(userController));
        registry.register(MessageType.UPDATE_PROFILE, new UpdateProfileCommand(userController));
        registry.register(MessageType.USER_UPDATE_AVATAR, new UserUpdateAvatarCommand(userController));
        registry.register(MessageType.ADMIN_GET_ALL_USERS, new AdminGetAllUsersCommand(adminController));
        registry.register(MessageType.ADMIN_BAN_USER, new AdminBanUserCommand(adminController));
        registry.register(MessageType.ADMIN_CANCEL_AUCTION, new AdminCancelAuctionCommand(adminController));
        
        // Financial Commands
        registry.register(MessageType.DEPOSIT, new DepositCommand(financeController));
        registry.register(MessageType.WITHDRAW, new WithdrawCommand(financeController));
        registry.register(MessageType.TRANSFER, new TransferCommand(financeController));

        // 4. Start Auction Monitor (Background task)
        AuctionMonitor auctionMonitor = new AuctionMonitor(productService);
        // 5. Start Socket Server
        SocketServer server = new SocketServer(registry, authService, auctionMonitor);
        server.run();
    }
}
