package org.example.server.service.user;

import org.example.model.Auction;
import org.example.server.repository.TransactionManager;
import org.example.server.repository.WatchlistDao;

import java.util.List;

/**
 * Service for managing user watchlists (product-centric).
 */
public class WatchlistService {
    private final WatchlistDao watchlistDao;
    private final org.example.server.repository.ProductDao productDao;
    private final TransactionManager txManager;

    public WatchlistService(TransactionManager txManager) {
        this.watchlistDao = WatchlistDao.getInstance();
        this.productDao = org.example.server.repository.ProductDao.getInstance();
        this.txManager = txManager;
    }

    public boolean addToWatchlist(String accountname, int productId) {
        return txManager.execute(conn -> {
            org.example.model.product.Product product = productDao.getProductById(conn, productId);
            if (product == null) {
                throw new org.example.server.exception.NotFoundException("Product not found");
            }
            if (product.getOwnerAccountname().equals(accountname)) {
                throw new org.example.server.exception.ValidationException("You already own this product");
            }
            return watchlistDao.addToWatchlist(conn, accountname, productId);
        });
    }

    public boolean removeFromWatchlist(String accountname, int productId) {
        return txManager.execute(conn -> watchlistDao.removeFromWatchlist(conn, accountname, productId));
    }

    public List<Auction> getWatchlist(String accountname) {
        return txManager.query(conn -> watchlistDao.getWatchedProductsWithLatestStatus(conn, accountname));
    }

    public List<String> getUsersWatchingProduct(int productId) {
        return txManager.query(conn -> watchlistDao.getUsersWatchingProduct(conn, productId));
    }
}
