package org.example.server.service.bid.strategy;

import org.example.model.Auction;

import java.sql.Connection;
import java.sql.SQLException;

public interface BidStrategy {
    void execute(Connection connection, Auction auction, String bidderAccountname, long amount) throws SQLException;
}
