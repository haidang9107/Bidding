package org.example.server.network.command;

import org.example.dto.request.AuctionOpenRequest;
import org.example.model.enums.MessageType;
import org.example.model.user.User;
import org.example.payload.Request;
import org.example.payload.Response;
import org.example.server.exception.ValidationException;
import org.example.server.network.SessionManager;
import org.example.server.service.auction.AuctionService;
import org.example.util.JsonConverter;

import java.nio.channels.SocketChannel;

/**
 * Command for opening an auction on a product the user already owns.
 *
 * <p>Two-step seller flow: PRODUCT_CREATE puts the product in the seller's
 * inventory; AUCTION_OPEN then puts it on the marketplace with price and
 * schedule.
 */
public class AuctionOpenCommand implements Command {

    private final AuctionService auctionService;

    public AuctionOpenCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response<?> execute(Request request, SocketChannel channel) {
        User currentUser = SessionManager.getUser(channel);
        if (currentUser == null) {
            return new Response<>(MessageType.ERROR, false, "Unauthorized", null);
        }
        AuctionOpenRequest req = JsonConverter.convert(request.getPayload(), AuctionOpenRequest.class);
        if (req == null) {
            return new Response<>(MessageType.ERROR, false, "Invalid payload", null);
        }
        if (req.getProductId() <= 0) {
            throw new ValidationException("Thiếu mã sản phẩm");
        }
        if (req.getStartingPrice() <= 0) {
            throw new ValidationException("Giá khởi điểm phải lớn hơn 0");
        }

        auctionService.openAuctionForProduct(
                req.getProductId(),
                currentUser.getAccountname(),
                req.getStartingPrice(),
                req.getStepPrice(),
                req.getBuyNowPrice(),
                req.getStartTime(),
                req.getEndTime()
        );
        return new Response<>(MessageType.SUCCESS, true,
                "Đã đăng sản phẩm lên sàn đấu giá", null);
    }
}
