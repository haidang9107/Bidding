package org.example.server.service.auction;

import org.example.dto.request.ProductAddRequest;
import org.example.dto.response.PagedResponse;
import org.example.model.Auction;
import org.example.model.enums.AuctionStatus;
import org.example.model.enums.TransactionType;
import org.example.model.product.ItemFactory;
import org.example.model.product.Product;
import org.example.server.event.AuctionCreatedEvent;
import org.example.server.event.AuctionEndedEvent;
import org.example.server.event.AuctionStartedEvent;
import org.example.server.event.EventPublisher;
import org.example.server.exception.AuctionException;
import org.example.server.exception.ValidationException;
import org.example.server.repository.AuctionDao;
import org.example.server.repository.ProductDao;
import org.example.server.repository.TransactionDao;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.UserDao;
import org.example.server.service.product.ProductService;
import org.example.util.FileLogger;
import org.example.util.JsonConverter;

import java.sql.Timestamp;
import java.util.List;

/**
 * Service handling the auction session lifecycle: creating, starting,
 * finishing, and cancelling auctions. Knows nothing about how bids are
 * applied (that is {@code BidService}'s job).
 */
public class AuctionService {
    private static final long DEFAULT_AUCTION_DURATION_MS = 7L * 24 * 60 * 60 * 1000;

    private final AuctionDao auctionDao;
    private final ProductDao productDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final ProductService productService;
    private final TransactionManager txManager;
    private final EventPublisher eventPublisher;
    private AuctionMonitor auctionMonitor;

    /**
     * Constructs an AuctionService.
     * @param txManager      The transaction manager.
     * @param eventPublisher The event publisher.
     * @param productService The product service (used to create the underlying product).
     */
    public AuctionService(TransactionManager txManager, EventPublisher eventPublisher,
                          ProductService productService) {
        this.auctionDao = new AuctionDao();
        this.productDao = new ProductDao();
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.productService = productService;
        this.txManager = txManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Wires in the auction monitor. Must be called after construction to break the
     * circular dependency (the monitor itself needs the service).
     * @param auctionMonitor The auction monitor.
     */
    public void setAuctionMonitor(AuctionMonitor auctionMonitor) {
        this.auctionMonitor = auctionMonitor;
    }

    /**
     * Retrieves a single auction (with its product) by ID.
     * @param auctionId The auction ID.
     * @return The auction, or null if not found.
     */
    public Auction getAuctionById(int auctionId) {
        return txManager.query(conn -> auctionDao.getAuctionById(conn, auctionId));
    }

    /**
     * Retrieves a paged list of auctions (products eagerly loaded).
     * @param page     The 1-based page number.
     * @param pageSize The page size.
     * @return A paged response of auctions.
     */
    public PagedResponse<Auction> getAuctionsPaged(int page, int pageSize) {
        return txManager.query(conn -> auctionDao.getAuctionsPagedResponse(conn, page, pageSize));
    }

    /**
     * Retrieves all currently running auctions.
     * @return A list of running auctions.
     */
    public List<Auction> getAllRunningAuctions() {
        return txManager.query(auctionDao::getRunningAuctions);
    }

    /**
     * Processes auctions whose end time has passed (finish each of them).
     */
    public void processExpiredAuctions() {
        List<Auction> expired = txManager.query(auctionDao::getExpiredAuctions);
        expired.forEach(a -> finishAuction(a.getAuctionId()));
    }

    /**
     * Processes auctions that are scheduled to start (start each of them).
     */
    public void processUpcomingAuctions() {
        List<Auction> upcoming = txManager.query(auctionDao::getUpcomingAuctions);
        upcoming.forEach(a -> startAuction(a.getAuctionId()));
    }

    /**
     * Creates a new product and opens its first auction.
     * @param addReq        The request describing the product and auction.
     * @param sellerAccount The seller's account name.
     */
    public void createAuction(ProductAddRequest addReq, String sellerAccount) {
        if (addReq == null) throw new ValidationException("Product data is required");

        // 0. If the client uploaded an image as Base64, persist it to disk and
        //    record the on-disk URL on the request so it gets stored with the product.
        String imageUrl = saveUploadedImageIfPresent(addReq, sellerAccount);

        txManager.run(conn -> {
            // 1. Build and insert the product
            Product product = ItemFactory.createProduct(
                    addReq.getCategory(),
                    JsonConverter.toJson(addReq)
            );
            product.setOwnerAccountname(sellerAccount);
            if (imageUrl != null) {
                product.setImageUrl(imageUrl);
            }
            if (!productDao.insertProduct(conn, product)) {
                throw new AuctionException("Failed to insert product into database");
            }

            // 2. Build and insert the auction
            Auction auction = new Auction();
            auction.setProductId(product.getProductId());
            auction.setSellerAccountname(sellerAccount);
            auction.setStartingPrice(addReq.getStartingPrice());
            auction.setCurrentPrice(addReq.getStartingPrice());
            auction.setStepPrice(addReq.getStepPrice() != null
                    ? addReq.getStepPrice()
                    : Math.max(1, addReq.getStartingPrice() / 10));
            auction.setBuyNowPrice(addReq.getBuyNowPrice());
            auction.setStartTime(addReq.getStartTime() != null
                    ? addReq.getStartTime()
                    : new Timestamp(System.currentTimeMillis()));
            auction.setEndTime(addReq.getEndTime() != null
                    ? addReq.getEndTime()
                    : new Timestamp(System.currentTimeMillis() + DEFAULT_AUCTION_DURATION_MS));
            auction.setStatus(AuctionStatus.OPEN);
            auction.setProduct(product);

            if (!auctionDao.insertAuction(conn, auction)) {
                throw new AuctionException("Failed to insert auction into database");
            }

            // 3. Schedule the start/end of this auction
            if (auctionMonitor != null) {
                if (auction.getStatus() == AuctionStatus.OPEN) {
                    auctionMonitor.scheduleAuctionStart(auction.getAuctionId(), auction.getStartTime());
                } else if (auction.getStatus() == AuctionStatus.RUNNING) {
                    auctionMonitor.scheduleAuctionEnd(auction.getAuctionId(), auction.getEndTime());
                }
            }
            eventPublisher.publish(new AuctionCreatedEvent(auction.getAuctionId()));
        });
    }

    /**
     * Opens an auction on an existing inventory product. Use this for the
     * two-step flow: a product is created first (via {@link ProductService#createInventoryProduct})
     * and only later put up for auction with price + schedule.
     *
     * @param productId      The inventory product ID.
     * @param sellerAccount  The caller's account name (must own the product).
     * @param startingPrice  The starting price.
     * @param stepPrice      The bid increment (may be null → defaults to startingPrice / 10).
     * @param buyNowPrice    Optional buy-now price (nullable).
     * @param startTime      Optional start time (nullable → now).
     * @param endTime        Optional end time (nullable → start + default duration).
     */
    public void openAuctionForProduct(int productId, String sellerAccount,
                                      long startingPrice, Long stepPrice, Long buyNowPrice,
                                      Timestamp startTime, Timestamp endTime) {
        if (startingPrice <= 0) {
            throw new ValidationException("Starting price must be positive");
        }

        txManager.run(conn -> {
            Product product = productDao.getProductForUpdate(conn, productId);
            if (product == null) {
                throw new org.example.server.exception.NotFoundException(
                        "Product not found: " + productId);
            }
            if (!sellerAccount.equalsIgnoreCase(product.getOwnerAccountname())) {
                throw new ValidationException("You do not own this product");
            }
            if (product.isInAuction()) {
                throw new AuctionException("Product is already in auction");
            }

            // Flip the inventory flag so the product can no longer be opened twice.
            productDao.updateProductAuctionFlag(conn, productId, true);

            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp effectiveStart = (startTime != null) ? startTime : now;
            Timestamp effectiveEnd = (endTime != null) ? endTime
                    : new Timestamp(effectiveStart.getTime() + DEFAULT_AUCTION_DURATION_MS);
            if (!effectiveEnd.after(effectiveStart)) {
                throw new ValidationException("End time must be after start time");
            }

            Auction auction = new Auction();
            auction.setProductId(productId);
            auction.setSellerAccountname(sellerAccount);
            auction.setStartingPrice(startingPrice);
            auction.setCurrentPrice(startingPrice);
            auction.setStepPrice(stepPrice != null ? stepPrice
                    : Math.max(1L, startingPrice / 10));
            auction.setBuyNowPrice(buyNowPrice);
            auction.setStartTime(effectiveStart);
            auction.setEndTime(effectiveEnd);
            auction.setStatus(AuctionStatus.OPEN);
            auction.setProduct(product);

            if (!auctionDao.insertAuction(conn, auction)) {
                throw new AuctionException("Failed to insert auction into database");
            }

            if (auctionMonitor != null) {
                auctionMonitor.scheduleAuctionStart(auction.getAuctionId(), effectiveStart);
            }
            eventPublisher.publish(new AuctionCreatedEvent(auction.getAuctionId()));
        });
    }

    private String saveUploadedImageIfPresent(ProductAddRequest addReq, String sellerAccount) {
        String b64 = addReq.getImageBase64();
        if (b64 == null || b64.isEmpty()) return null;
        try {
            // Tolerate "data:image/png;base64,..." prefixes
            String pure = b64;
            String mimeFromDataUrl = null;
            int comma = pure.indexOf(',');
            if (pure.startsWith("data:") && comma > 0) {
                String header = pure.substring(5, comma); // e.g. "image/png;base64"
                int semi = header.indexOf(';');
                mimeFromDataUrl = (semi > 0) ? header.substring(0, semi) : header;
                pure = pure.substring(comma + 1);
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(pure);

            String mime = addReq.getImageMimeType();
            if (mime == null || mime.isEmpty()) mime = mimeFromDataUrl;
            String ext = guessExtension(mime);

            java.nio.file.Path dir = java.nio.file.Paths.get("uploads", "products");
            java.nio.file.Files.createDirectories(dir);
            String filename = "p_" + sellerAccount + "_" + System.currentTimeMillis() + "." + ext;
            java.nio.file.Path target = dir.resolve(filename);
            java.nio.file.Files.write(target, bytes);

            // Clear payload so we don't shove the Base64 blob into the Product subclass
            addReq.setImageBase64(null);
            return target.toUri().toString();
        } catch (Exception e) {
            FileLogger.warn("Failed to save uploaded product image: " + e.getMessage());
            return null;
        }
    }

    private String guessExtension(String mime) {
        if (mime == null) return "png";
        return switch (mime.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif"               -> "gif";
            case "image/webp"              -> "webp";
            case "image/bmp"               -> "bmp";
            default                        -> "png";
        };
    }

    /**
     * Transitions an auction from OPEN to RUNNING.
     * @param auctionId The auction ID.
     */
    public void startAuction(int auctionId) {
        txManager.run(conn -> {
            Auction auction = auctionDao.getAuctionForUpdate(conn, auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.OPEN) return;

            boolean updated = auctionDao.updateStatus(conn, auctionId, AuctionStatus.RUNNING);
            if (updated) {
                FileLogger.info("Auction STARTED: Auction ID " + auctionId);
                if (auctionMonitor != null) {
                    auctionMonitor.scheduleAuctionEnd(auctionId, auction.getEndTime());
                }
                String productName = auction.getProduct() != null ? auction.getProduct().getName() : "";
                eventPublisher.publish(new AuctionStartedEvent(auctionId, productName));
            }
        });
    }

    /**
     * Alias for {@link #finishAuction(int)} kept for AuctionMonitor compatibility.
     * @param auctionId The auction ID.
     */
    public void processAuctionEnd(int auctionId) {
        finishAuction(auctionId);
    }

    /**
     * Finishes an auction: settles payment, transfers product ownership, publishes an event.
     * @param auctionId The auction ID.
     */
    public void finishAuction(int auctionId) {
        txManager.run(conn -> {
            Auction auction = auctionDao.getAuctionForUpdate(conn, auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

            String winner = auction.getWinnerAccountname();
            String seller = auction.getSellerAccountname();
            long finalPrice = auction.getCurrentPrice();
            int productId = auction.getProductId();
            String productName = auction.getProduct() != null ? auction.getProduct().getName() : "";

            boolean success = auctionDao.updateStatus(conn, auctionId, AuctionStatus.FINISHED);
            if (success && winner != null) {
                userDao.addBalance(conn, winner, -finalPrice);
                userDao.addBlockedBalance(conn, winner, -finalPrice);
                userDao.addBalance(conn, seller, finalPrice);
                productService.transferOwnership(conn, productId, winner);

                transactionDao.insertTransaction(conn, winner, seller,
                        TransactionType.AUCTION_PAYMENT, productId, finalPrice,
                        auctionId, "Payment for auction win: " + productName);
            } else if (success) {
                // No winner: just release the product from "in-auction"
                productDao.updateProductAuctionFlag(conn, productId, false);
            }

            if (success) {
                FileLogger.info("Auction FINISHED: Auction ID " + auctionId
                        + (winner != null ? ", Winner: " + winner : " (No winner)"));
                eventPublisher.publish(new AuctionEndedEvent(
                        auctionId, productName, winner, finalPrice, false));
            }
        });
    }

    /**
     * Cancels an auction and releases any blocked balance held by the current leader.
     * @param auctionId The auction ID.
     * @return True if the auction was successfully cancelled.
     */
    public boolean cancelAuction(int auctionId) {
        return txManager.execute(conn -> {
            Auction auction = auctionDao.getAuctionForUpdate(conn, auctionId);
            if (auction == null
                    || auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return false;
            }

            boolean success = auctionDao.updateStatus(conn, auctionId, AuctionStatus.CANCELED);
            if (success) {
                productDao.updateProductAuctionFlag(conn, auction.getProductId(), false);
                if (auction.getWinnerAccountname() != null) {
                    userDao.addBlockedBalance(conn, auction.getWinnerAccountname(),
                            -auction.getCurrentPrice());
                }
                FileLogger.info("Admin action: Auction " + auctionId + " has been CANCELED.");
                String productName = auction.getProduct() != null ? auction.getProduct().getName() : "";
                eventPublisher.publish(new AuctionEndedEvent(
                        auctionId, productName,
                        auction.getWinnerAccountname(), auction.getCurrentPrice(), true));
            }
            return success;
        });
    }
}
