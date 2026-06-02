package org.example.server;

import org.example.model.enums.MessageType;
import org.example.server.controller.*;
import org.example.server.event.EventPublisher;
import org.example.server.event.NetworkNotificationListener;
import org.example.server.network.DisconnectionHandler;
import org.example.server.network.SocketServer;
import org.example.server.network.command.*;
import org.example.server.repository.DatabaseConnectionPool;
import org.example.server.repository.TransactionManager;
import org.example.server.service.auction.AuctionMonitor;
import org.example.server.service.auction.AuctionService;
import org.example.server.service.bid.BidService;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransactionService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.server.service.product.ProductService;
import org.example.server.service.user.UserService;
import org.example.server.service.user.WatchlistService;
import org.example.server.service.user.admin.AdminService;
import org.example.server.service.user.auth.AuthService;
import org.example.util.FileLogger;

/**
 * Main entry point for the Bidding Server.
 * Refactored to use non-static components, Event-Driven Architecture, and a clean
 * Product / Auction separation.
 */
public class ServerApp {

    /**
     * Main entry point.
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        try {
            bootstrap();
        } catch (Exception e) {
            e.printStackTrace();
            FileLogger.error("CRITICAL: Server failed to start", e);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            System.exit(1);
        }
    }

    /**
     * Bootstraps the server: infrastructure → services → controllers → network.
     * @throws Exception if an error occurs during bootstrapping.
     */
    private static void bootstrap() throws Exception {
        FileLogger.info("Starting Bidding Server Bootstrap Sequence...");

        // 1. Infrastructure
        DatabaseConnectionPool pool = new DatabaseConnectionPool();
        TransactionManager txManager = new TransactionManager(pool);
        EventPublisher eventPublisher = new EventPublisher();

        // 2. Services - Product first (used by AuctionService for ownership transfer)
        ProductService productService = new ProductService(txManager, eventPublisher);
        AuctionService auctionService = new AuctionService(txManager, eventPublisher, productService);

        // 3. AuctionMonitor depends on AuctionService; wired back via setter to break the cycle
        AuctionMonitor auctionMonitor = new AuctionMonitor(auctionService);
        auctionService.setAuctionMonitor(auctionMonitor);

        // 4. Other services
        AuthService authService = new AuthService(txManager);
        BidService bidService = new BidService(txManager, eventPublisher, auctionMonitor);
        UserService userService = new UserService(txManager);
        WatchlistService watchlistService = new WatchlistService(txManager);
        auctionMonitor.setWatchlistService(watchlistService);
        AdminService adminService = new AdminService(txManager, auctionService);
        DepositService depositService = new DepositService(txManager, eventPublisher);
        WithdrawService withdrawService = new WithdrawService(txManager, eventPublisher);
        TransferService transferService = new TransferService(txManager, eventPublisher);
        TransactionService transactionService = new TransactionService(txManager);

        // 5. Notification & Connection wiring
        NetworkNotificationListener notifListener = new NetworkNotificationListener(auctionService);
        notifListener.registerAll(eventPublisher);
        DisconnectionHandler disconnectionHandler = new DisconnectionHandler(txManager);

        // 6. Controllers
        AuthController authController = new AuthController(authService);
        AuctionController auctionController = new AuctionController(auctionService);
        BidController bidController = new BidController(bidService);
        AdminController adminController = new AdminController(adminService);
        UserController userController = new UserController(userService);
        FinanceController financeController = new FinanceController(
                depositService, withdrawService, transferService, transactionService);

        // 7. Command Registry
        CommandRegistry registry = new CommandRegistry();
        registerCommands(registry, authController, auctionController, bidController,
                adminController, userController, financeController, auctionService, productService, 
                watchlistService, disconnectionHandler);

        // 8. Socket Server
        SocketServer server = new SocketServer(registry, authService, auctionMonitor, disconnectionHandler);

        // 8.1 Inactivity Monitor
        org.example.server.network.InactivityMonitor inactivityMonitor =
                new org.example.server.network.InactivityMonitor(60, disconnectionHandler);

        // 9. Start Background Tasks
        auctionMonitor.start();
        inactivityMonitor.start();

        // 10. Graceful Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FileLogger.info("Shutdown signal received. Cleaning up resources...");
            inactivityMonitor.stop();
            auctionMonitor.stop();
            server.stop();
            eventPublisher.shutdown();
            pool.close();
            FileLogger.info("Server shutdown complete.");
        }));

        // 11. Start
        server.run();
    }

    /**
     * Registers all available commands with the command registry.
     */
    private static void registerCommands(
            CommandRegistry registry,
            AuthController auth, AuctionController auction,
            BidController bid, AdminController admin,
            UserController user, FinanceController finance,
            AuctionService auctionService, ProductService productService,
            WatchlistService watchlistService,
            DisconnectionHandler disconnectionHandler) {

        // Auth
        registry.register(MessageType.LOGIN,  new LoginCommand(auth, disconnectionHandler));
        registry.register(MessageType.SIGNUP, new SignupCommand(auth));
        registry.register(MessageType.LOGOUT, new LogoutCommand());
        registry.register(MessageType.PING,   new PingCommand());

        // Auction (product list / detail / create / search)
        registry.register(MessageType.PRODUCT_LIST,       new AuctionListCommand(auction));
        registry.register(MessageType.PRODUCT_SEARCH,     new ProductSearchCommand(auction));
        registry.register(MessageType.PRODUCT_DETAIL,     new AuctionDetailCommand(auction));
        registry.register(MessageType.PRODUCT_ADD,        new AuctionCreateCommand(auction));
        registry.register(MessageType.PRODUCT_CREATE,     new ProductCreateCommand(productService));
        registry.register(MessageType.PRODUCT_UPDATE,     new ProductUpdateCommand(productService));
        registry.register(MessageType.PRODUCT_WITHDRAW,   new ProductWithdrawCommand(productService));
        registry.register(MessageType.MY_PRODUCT_LIST,    new MyProductListCommand(productService));
        registry.register(MessageType.AUCTION_OPEN,       new AuctionOpenCommand(auctionService));
        registry.register(MessageType.JOIN_AUCTION_ROOM,  new JoinAuctionRoomCommand(auctionService));
        registry.register(MessageType.LEAVE_AUCTION_ROOM, new LeaveAuctionRoomCommand());

        // Watchlist
        WatchlistCommand watchlistCmd = new WatchlistCommand(watchlistService);
        registry.register(MessageType.WATCHLIST_ADD,    watchlistCmd);
        registry.register(MessageType.WATCHLIST_REMOVE, watchlistCmd);
        registry.register(MessageType.WATCHLIST_GET,    watchlistCmd);

        // Bidding
        registry.register(MessageType.BID_PLACE,       new BidPlaceCommand(bid));
        registry.register(MessageType.AUTO_BID_SET,    new AutoBidSetCommand(bid));
        registry.register(MessageType.AUTO_BID_CANCEL, new AutoBidCancelCommand(bid));
        registry.register(MessageType.BID_HISTORY,     new BidHistoryCommand(bid));

        // User & Admin
        registry.register(MessageType.GET_PROFILE,        new GetProfileCommand(user));
        registry.register(MessageType.UPDATE_PROFILE,     new UpdateProfileCommand(user));
        registry.register(MessageType.UPDATE_PASSWORD,    new UpdatePasswordCommand(user));
        registry.register(MessageType.USER_UPDATE_AVATAR, new UserUpdateAvatarCommand(user));
        registry.register(MessageType.ADMIN_GET_ALL_USERS,  new AdminGetAllUsersCommand(admin));
        registry.register(MessageType.ADMIN_BAN_USER,       new AdminBanUserCommand(admin, disconnectionHandler));
        registry.register(MessageType.ADMIN_CANCEL_AUCTION, new AdminCancelAuctionCommand(admin));
        registry.register(MessageType.ADMIN_GET_STATS,      new AdminGetStatsCommand(admin));

        // Finance
        registry.register(MessageType.DEPOSIT,             new DepositCommand(finance));
        registry.register(MessageType.WITHDRAW,            new WithdrawCommand(finance));
        registry.register(MessageType.TRANSFER,            new TransferCommand(finance));
        registry.register(MessageType.TRANSACTION_HISTORY, new TransactionHistoryCommand(finance));
    }
}
