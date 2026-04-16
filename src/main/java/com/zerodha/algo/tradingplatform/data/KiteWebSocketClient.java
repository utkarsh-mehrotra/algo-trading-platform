package com.zerodha.algo.tradingplatform.data;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder for the actual Kite WebSocket decoding logic.
 * Kite WebSocket sends binary frames which need to be unpacked according to their protocol.
 */
@Slf4j
public class KiteWebSocketClient extends WebSocketClient {

    private final TickProcessor tickProcessor;

    public KiteWebSocketClient(URI serverUri, TickProcessor tickProcessor) {
        super(serverUri);
        this.tickProcessor = tickProcessor;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to Kite WebSocket API");
    }

    @Override
    public void onMessage(String message) {
        // String messages are usually errors or heartbeats from Kite
        log.debug("Received text message: {}", message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // Here we parse the Kite Connect Binary packets into our Tick objects.
        // Assuming TickProcessor.decode() does the binary unmarshaling.
        tickProcessor.processRawTicks(bytes);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("Disconnected from Kite WebSocket. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
        // Retries will be handled by a wrapper service with exponential backoff
    }

    @Override
    public void onError(Exception ex) {
        log.error("Kite WebSocket Error", ex);
    }
}
