package org.example.server;

import org.example.server.controller.AuthController;
import org.example.server.controller.BidController;
import org.example.server.controller.ProductController;
import org.example.server.network.SocketServer;
import org.example.server.network.command.*;
import org.example.server.repository.DatabaseManager;
import org.example.server.repository.ProductDao;
import org.example.server.repository.UserDao;
import org.example.server.service.bid.BidService;
import org.example.server.service.product.ProductService;
import org.example.server.service.user.auth.AuthService;
import org.example.server.service.finance.DepositService;
import org.example.server.service.finance.TransferService;
import org.example.server.service.finance.WithdrawService;
import org.example.model.enums.MessageType;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Main Entry point for the Bidding Server.
 * Initializes all dependencies and starts the Socket Server.
 */
public class ServerApp {

    public static void main(String[] args) {
        FileLogger.info("Starting Bidding Server...");

        // 1. Initialize Services (They now manage their own connections from the pool)
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

        // 3. Initialize Command Registry (SOLID & Command Pattern)
        CommandRegistry registry = new CommandRegistry();
        registry.register(MessageType.LOGIN, new LoginCommand(authController));
        registry.register(MessageType.SIGNUP, new SignupCommand(authController));
        registry.register(MessageType.LOGOUT, new LogoutCommand());
        registry.register(MessageType.PING, new PingCommand());
        registry.register(MessageType.PRODUCT_LIST, new ProductListCommand(productController));
        registry.register(MessageType.PRODUCT_ADD, new ProductAddCommand(productController));
        registry.register(MessageType.BID_PLACE, new BidPlaceCommand(bidController));
        
        // Financial Commands
        registry.register(MessageType.DEPOSIT, new DepositCommand(depositService));
        registry.register(MessageType.WITHDRAW, new WithdrawCommand(withdrawService));
        registry.register(MessageType.TRANSFER, new TransferCommand(transferService));

        // 4. Start Socket Server
        SocketServer server = new SocketServer(registry);
        server.run();
    }
}
