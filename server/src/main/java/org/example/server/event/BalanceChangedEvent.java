package org.example.server.event;

/**
 * Event triggered when a user's balance or blocked balance changes.
 */
public record BalanceChangedEvent(String accountname, long newBalance, long newBlockedBalance) implements DomainEvent {
}
