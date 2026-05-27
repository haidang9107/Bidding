package org.example.server.service.bid.strategy;

import org.example.model.Auction;
import org.example.server.repository.AuctionDao;
import org.example.util.FileLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BuyNowBidStrategy implements BidStrategy {
    private final AuctionDao auctionDao;

    public BuyNowBidStrategy(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    @Override
    public void execute(Connection connection, Auction auction, String bidderAccountname, long amount) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        auction.setEndTime(now);
        auctionDao.updateAuctionEndTime(connection, auction.getAuctionId(), now);
        FileLogger.info("Buy Now triggered for Auction " + auction.getAuctionId() + " by " + bidderAccountname);
    }
}
