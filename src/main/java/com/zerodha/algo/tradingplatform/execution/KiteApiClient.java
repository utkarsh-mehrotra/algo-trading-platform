package com.zerodha.algo.tradingplatform.execution;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiteApiClient {

    private final OkHttpClient httpClient = new OkHttpClient();

    @Retry(name = "kiteApi")
    public String submitOrder(OrderEntity entity) {
        log.info("Attempting to submit order via REST API: {} qty {}", entity.getInstrumentToken(), entity.getTotalQty());
        // Simulating HTTP call
        return "KITE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Retry(name = "kiteApi")
    public void cancelOrder(String brokerOrderId) {
        log.info("Cancelling order {}", brokerOrderId);
    }

    @Retry(name = "kiteApi")
    public Map<String, String> fetchOrderBookStatus() {
        // Used for reconciliation
        return Collections.emptyMap();
    }
}
