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
import org.example.server.repository.DatabaseManager;
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
        try {
            bootstrap();
        } catch (Exception e) {
            FileLogger.error("CRITICAL: Server failed to start", e);
            System.exit(1);
        }
    }

    private static void bootstrap() {
        FileLogger.info("Starting Bidding Server Bootstrap Sequence...");

        // 0. Initialize Database
        DatabaseManager.init();

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
        registerCommands(registry, authController, productController, bidController, adminController, userController, financeController, productService);

        // 4. Start Background Tasks
        AuctionMonitor auctionMonitor = new AuctionMonitor(productService);
        
        // 5. Initialize Socket Server
        SocketServer server = new SocketServer(registry, authService, auctionMonitor);

        // 6. Register Shutdown Hook for Graceful Cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileLogger.info("Shutdown signal received. Cleaning up resources...");
            server.stop();
            FileLogger.info("Server shutdown complete.");
        }));

        // 7. Start Server
        server.run();
    }

    private static void registerCommands(CommandRegistry registry, AuthController auth, ProductController product,
                                         BidController bid, AdminController admin, UserController user, 
                                         FinanceController finance, ProductService productService) {
        // Auth Commands
        registry.register(MessageType.LOGIN, new LoginCommand(auth));
        registry.register(MessageType.SIGNUP, new SignupCommand(auth));
        registry.register(MessageType.LOGOUT, new LogoutCommand());
        registry.register(MessageType.PING, new PingCommand());

        // Product & Auction Commands
        registry.register(MessageType.PRODUCT_LIST, new ProductListCommand(product));
        registry.register(MessageType.PRODUCT_DETAIL, new ProductDetailCommand(product));
        registry.register(MessageType.PRODUCT_ADD, new ProductAddCommand(product));
        registry.register(MessageType.JOIN_AUCTION_ROOM, new JoinAuctionRoomCommand(productService));
        registry.register(MessageType.LEAVE_AUCTION_ROOM, new LeaveAuctionRoomCommand());

        // Bidding Commands
        registry.register(MessageType.BID_PLACE, new BidPlaceCommand(bid));
        registry.register(MessageType.AUTO_BID_SET, new AutoBidSetCommand(bid));
        registry.register(MessageType.AUTO_BID_CANCEL, new AutoBidCancelCommand(bid));

        // User & Admin Commands
        registry.register(MessageType.GET_PROFILE, new GetProfileCommand(user));
        registry.register(MessageType.UPDATE_PROFILE, new UpdateProfileCommand(user));
        registry.register(MessageType.USER_UPDATE_AVATAR, new UserUpdateAvatarCommand(user));
        registry.register(MessageType.ADMIN_GET_ALL_USERS, new AdminGetAllUsersCommand(admin));
        registry.register(MessageType.ADMIN_BAN_USER, new AdminBanUserCommand(admin));
        registry.register(MessageType.ADMIN_CANCEL_AUCTION, new AdminCancelAuctionCommand(admin));

        // Financial Commands
        registry.register(MessageType.DEPOSIT, new DepositCommand(finance));
        registry.register(MessageType.WITHDRAW, new WithdrawCommand(finance));
        registry.register(MessageType.TRANSFER, new TransferCommand(finance));
    }
}
