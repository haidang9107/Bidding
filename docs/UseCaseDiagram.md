# Use Case Diagram

This document illustrates the interactions between different actors and the Bidding System. It visualizes the core functional requirements and the boundaries of user capabilities.

## 1. System Actors

The system identifies three primary actors:
*   **Member**: A standard registered user who interacts with the core bidding functionalities (buying, selling, managing funds).
*   **Admin**: A privileged user responsible for system oversight, user management, and moderation.
*   **System Timer**: An automated background actor that triggers time-sensitive events independently of user interaction.

## 2. Use Case Diagram

```mermaid
graph LR
    %% Actors
    subgraph Actors
        M[Member]
        A[Admin]
        S[System Timer]
    end

    %% Use Cases
    subgraph "Bidding System Core Operations"
        %% Common Use Cases
        UC_Auth(("Authentication<br>Login/Signup"))
        
        %% Member Specific Use Cases
        UC_View(("Browse/Search<br>Auctions"))
        UC_Bid(("Place a Bid"))
        UC_AutoBid(("Configure<br>Auto-Bid"))
        UC_Wallet(("Manage Wallet<br>Top-up/Withdraw"))
        UC_Sell(("Create/Manage<br>Listings"))
        
        %% Admin Specific Use Cases
        UC_UserMgmt(("Manage/Ban Users"))
        UC_Moderate(("Moderate/Cancel<br>Auctions"))
        
        %% System Specific Use Cases
        UC_Expiry(("Monitor Auction<br>Expiry"))
        UC_Finalize(("Finalize/Close<br>Auction"))
    end

    %% Member Interactions
    M --- UC_Auth
    M --- UC_View
    M --- UC_Bid
    M --- UC_AutoBid
    M --- UC_Wallet
    M --- UC_Sell

    %% Admin Interactions
    A --- UC_Auth
    A --- UC_UserMgmt
    A --- UC_Moderate

    %% System Interactions
    S --- UC_Expiry
    
    %% Relationships & Dependencies
    UC_Expiry -. "<<include>>" .-> UC_Finalize
    UC_Moderate -. "<<include>>" .-> UC_Finalize
    UC_Bid -. "<<extend>>" .-> UC_AutoBid
```

## 3. Use Case Descriptions

### Member Actions
*   **Browse/Search Auctions**: Members can view active listings, read descriptions, and monitor current highest bids in real-time.
*   **Place a Bid**: Members can submit manual bids on active items. The system validates their account balance and freezes necessary funds (escrow).
*   **Configure Auto-Bid**: Members can set a maximum price and increment. The system will automatically place bids on their behalf.
*   **Manage Wallet**: Members can view their available balance, see funds locked in escrow, and simulate top-ups.
*   **Create Listings**: Members can put their own items up for auction, defining starting prices, categories, and optional "Buy Now" prices.

### Admin Actions
*   **Manage Users**: Admins have the authority to suspend or ban users who violate platform terms.
*   **Moderate Auctions**: Admins can intervene and forcefully cancel an auction if it contains inappropriate content or fraudulent activity.

### Automated System Actions
*   **Monitor Auction Expiry**: The internal time scheduler actively tracks the end time of all ongoing auctions.
*   **Finalize/Close Auction**: Triggered by either the Time Scheduler (time expired) or an Admin (forced cancellation). This use case resolves the winner, transfers funds from escrow to the seller, and notifies all participants.
