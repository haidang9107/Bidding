package org.example.client.network;

import org.example.payload.Response;

/**
 * Interface để controller đăng ký nhận message từ server.
 * (Observer pattern phía client.)
 */
@FunctionalInterface
public interface ServerListener {
    void onMessage(Response response);
}
