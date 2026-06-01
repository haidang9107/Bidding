package org.example.dto.response;

import java.io.Serializable;

/**
 * DTO for system-wide statistics for administrators.
 */
public record AdminStatsResponse(
    long totalUsers,
    long activeUsers,
    long bannedUsers,
    long totalProducts,
    long productsInInventory,
    long productsInAuction,
    long activeAuctions,
    long completedAuctions,
    long canceledAuctions,
    long totalTransactions,
    long totalTransactionVolume
) implements Serializable {
}
