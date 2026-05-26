package org.example.server.service.bid.strategy;

import org.example.model.Auction;
import org.example.server.repository.AuctionDao;
import org.example.server.service.auction.AntiSnipping;

import java.sql.Connection;
import java.sql.SQLException;

public class NormalBidStrategy implements BidStrategy {
    private final AuctionDao auctionDao;

    public NormalBidStrategy(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    @Override
    public void execute(Connection connection, Auction auction, String bidderAccountname, long amount) throws SQLException {
        AntiSnipping.process(connection, auction, auctionDao);
    }
}
