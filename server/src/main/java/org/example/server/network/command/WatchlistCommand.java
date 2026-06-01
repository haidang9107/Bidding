package org.example.server.network.command;

import com.google.gson.JsonObject;
import org.example.dto.response.ProductResponse;
import org.example.model.Auction;
import org.example.model.enums.MessageType;
import org.example.model.enums.UserRole;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.annotation.RequiresRole;
import org.example.server.network.SessionManager;
import org.example.server.service.user.WatchlistService;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for managing a user's watchlist (tracking product_id).
 * Supports adding, removing, and retrieving the watchlist.
 */
@RequiresRole(UserRole.MEMBER)
public class WatchlistCommand implements Command {
    private final WatchlistService watchlistService;

    /**
     * Constructs a WatchlistCommand.
     * @param watchlistService The service for watchlist operations.
     */
    public WatchlistCommand(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /**
     * Executes the watchlist operation based on the request type.
     * @param request The request (WATCHLIST_ADD, WATCHLIST_REMOVE, or WATCHLIST_GET).
     * @param channel The user's socket channel.
     * @return A success or error response.
     */
    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User user = SessionManager.getUser(channel);
        if (user == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }

        MessageType type = request.getType();
        String accountname = user.getAccountname();

        if (type == MessageType.WATCHLIST_GET) {
            List<Auction> watchlist = watchlistService.getWatchlist(accountname);
            List<ProductResponse> items = watchlist.stream()
                    .map(ProductResponse::new)
                    .collect(Collectors.toList());
            return new Response<>(MessageType.WATCHLIST_GET, true, "Watchlist fetched", items);
        }

        JsonObject payload = JsonConverter.convert(request.getPayload(), JsonObject.class);
        if (payload == null || !payload.has("productId")) {
            return new Response<>(MessageType.ERROR, false, "Invalid payload: productId required", null);
        }

        int productId = payload.get("productId").getAsInt();

        if (type == MessageType.WATCHLIST_ADD) {
            boolean success = watchlistService.addToWatchlist(accountname, productId);
            return success 
                ? new Response<>(MessageType.SUCCESS, true, "Added to watchlist", null)
                : new Response<>(MessageType.ERROR, false, "Failed to add to watchlist", null);
        } else if (type == MessageType.WATCHLIST_REMOVE) {
            boolean success = watchlistService.removeFromWatchlist(accountname, productId);
            return success
                ? new Response<>(MessageType.SUCCESS, true, "Removed from watchlist", null)
                : new Response<>(MessageType.ERROR, false, "Failed to remove from watchlist", null);
        }

        return new Response<>(MessageType.ERROR, false, "Unsupported message type", null);
    }
}
