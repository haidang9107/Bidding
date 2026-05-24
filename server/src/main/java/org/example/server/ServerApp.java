package org.example.server;

import org.example.model.enums.MessageType;
import org.example.server.controller.*;
import org.example.server.event.EventPublisher;
import org.example.server.event.NetworkNotificationListener;
import org.example.server.network.AuctionMonitor;
import org.example.server.network.SocketServer;
import org.example.server.network.command.*;
import org.example.server.repository.DatabaseConnectionPool;
import org.example.server.repository.TransactionManager;
import org.example.server.service.bid.BidService;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.server.service.finance.TransactionService;
import org.example.server.service.product.ProductService;
import org.example.server.service.user.UserService;
import org.example.server.service.user.admin.AdminService;
import org.example.server.service.user.auth.AuthService;
import org.example.util.FileLogger;

/**
 * Main entry point for the Bidding Server.
 * Refactored to use non-static components and Event-Driven Architecture.
 */
public class ServerApp {

    /**
     * Main entry point for the Bidding Server.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            bootstrap();
        } catch (Exception e) {
            FileLogger.error("CRITICAL: Server failed to start", e);
            System.exit(1);
        }
    }

    /**
     * Bootstraps the server components, including infrastructure, services, controllers, and network wiring.
     * @throws Exception if an error occurs during bootstrapping
     */
    private static void bootstrap() throws Exception {
        FileLogger.info("Starting Bidding Server Bootstrap Sequence...");

        // 1. Infrastructure
        DatabaseConnectionPool pool = new DatabaseConnectionPool();
        TransactionManager txManager = new TransactionManager(pool);
        EventPublisher eventPublisher = new EventPublisher();

        // 2. Services
        AuthService authService = new AuthService(txManager);
        ProductService productService = new ProductService(txManager, eventPublisher);
        
        // 7. Background Tasks (Depends on ProductService)
        AuctionMonitor auctionMonitor = new AuctionMonitor(productService);
        productService.setAuctionMonitor(auctionMonitor);

        // 2. Services (Continued)
        BidService bidService = new BidService(txManager, eventPublisher, auctionMonitor);
        UserService userService = new UserService(txManager);
        AdminService adminService = new AdminService(txManager, eventPublisher);
        DepositService depositService = new DepositService(txManager);
        WithdrawService withdrawService = new WithdrawService(txManager);
        TransferService transferService = new TransferService(txManager);
        TransactionService transactionService = new TransactionService(txManager);

        // 4. Network notification wiring
        NetworkNotificationListener notifListener = new NetworkNotificationListener(productService);
        notifListener.registerAll(eventPublisher);

        // 5. Controllers
        AuthController authController = new AuthController(authService);
        ProductController productController = new ProductController(productService);
        BidController bidController = new BidController(bidService);
        AdminController adminController = new AdminController(adminService);
        UserController userController = new UserController(userService);
        FinanceController financeController = new FinanceController(depositService, withdrawService, transferService, transactionService);

        // 6. Command Registry
        CommandRegistry registry = new CommandRegistry();
        registerCommands(registry, authController, productController, bidController,
                adminController, userController, financeController, productService);

        // 8. Socket Server
        SocketServer server = new SocketServer(registry, authService, auctionMonitor);

        // 9. Start Background Tasks
        auctionMonitor.start();

        // 10. Graceful Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileLogger.info("Shutdown signal received. Cleaning up resources...");
            server.stop();
            pool.close();
            FileLogger.info("Server shutdown complete.");
        }));

        // 10. Start
        server.run();
    }

    /**
     * Registers all available commands with the command registry.
     * @param registry the command registry to register with
     * @param auth the auth controller
     * @param product the product controller
     * @param bid the bid controller
     * @param admin the admin controller
     * @param user the user controller
     * @param finance the finance controller
     * @param productService the product service
     */
    private static void registerCommands(
            CommandRegistry registry,
            AuthController auth, ProductController product,
            BidController bid, AdminController admin,
            UserController user, FinanceController finance,
            ProductService productService) {

        // Auth
        registry.register(MessageType.LOGIN,  new LoginCommand(auth));
        registry.register(MessageType.SIGNUP, new SignupCommand(auth));
        registry.register(MessageType.LOGOUT, new LogoutCommand());
        registry.register(MessageType.PING,   new PingCommand());

        // Product & Auction
        registry.register(MessageType.PRODUCT_LIST,       new ProductListCommand(product));
        registry.register(MessageType.PRODUCT_DETAIL,     new ProductDetailCommand(product));
        registry.register(MessageType.PRODUCT_ADD,        new ProductAddCommand(product));
        registry.register(MessageType.JOIN_AUCTION_ROOM,  new JoinAuctionRoomCommand(productService));
        registry.register(MessageType.LEAVE_AUCTION_ROOM, new LeaveAuctionRoomCommand());

        // Bidding
        registry.register(MessageType.BID_PLACE,      new BidPlaceCommand(bid));
        registry.register(MessageType.AUTO_BID_SET,   new AutoBidSetCommand(bid));
        registry.register(MessageType.AUTO_BID_CANCEL, new AutoBidCancelCommand(bid));

        // User & Admin
        registry.register(MessageType.GET_PROFILE,        new GetProfileCommand(user));
        registry.register(MessageType.UPDATE_PROFILE,     new UpdateProfileCommand(user));
        registry.register(MessageType.USER_UPDATE_AVATAR, new UserUpdateAvatarCommand(user));
        registry.register(MessageType.ADMIN_GET_ALL_USERS,    new AdminGetAllUsersCommand(admin));
        registry.register(MessageType.ADMIN_BAN_USER,         new AdminBanUserCommand(admin));
        registry.register(MessageType.ADMIN_CANCEL_AUCTION,   new AdminCancelAuctionCommand(admin));

        // Finance
        registry.register(MessageType.DEPOSIT,  new DepositCommand(finance));
        registry.register(MessageType.WITHDRAW, new WithdrawCommand(finance));
        registry.register(MessageType.TRANSFER, new TransferCommand(finance));
        registry.register(MessageType.TRANSACTION_HISTORY, new TransactionHistoryCommand(finance));
    }
}
