package com.zerodha.algo.tradingplatform.execution;

import com.zerodha.algo.tradingplatform.core.ExecutionEngine;
import com.zerodha.algo.tradingplatform.model.Order;
import com.zerodha.algo.tradingplatform.model.OrderStatus;
import com.zerodha.algo.tradingplatform.model.Signal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionEngineImpl implements ExecutionEngine {

    private final OrderRepository orderRepository;
    private final KiteApiClient kiteApiClient;

    @PostConstruct
    public void init() {
        log.info("Startup reconciliation of pending orders...");
        reconcilePendingOrders();
    }

    @Override
    @Transactional
    public Order placeOrder(Signal signal, int quantity) {
        OrderEntity entity = new OrderEntity();
        entity.setInternalOrderId(UUID.randomUUID().toString());
        entity.setInstrumentToken(signal.getInstrumentToken());
        entity.setTransactionType(signal.getTransactionType());
        entity.setOrderType(signal.getPrice() == null ? "MARKET" : "LIMIT");
        entity.setPrice(signal.getPrice());
        entity.setTotalQty(quantity);
        entity.setPendingQty(quantity);
        entity.setProduct("MIS"); // Enforced intraday
        entity.setStrategyId(signal.getStrategyId());
        
        // Handle AMO timing check
        if (isAmoWindow()) {
            entity.setStatus(OrderStatus.AMO_PENDING.name());
        } else {
            entity.setStatus(OrderStatus.PENDING.name());
        }
        
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // 1. Write-ahead log to Postgres
        orderRepository.save(entity);
        log.debug("Order {} persisted to DB in {} state", entity.getInternalOrderId(), entity.getStatus());

        // 2. Submit to Kite API with retries
        try {
            String brokerOrderId = kiteApiClient.submitOrder(entity);
            entity.setBrokerOrderId(brokerOrderId);
            
            if (entity.getStatus().equals(OrderStatus.PENDING.name())) {
                entity.setStatus(OrderStatus.OPEN.name());
            }
        } catch (Exception e) {
            log.error("Failed to place order to broker", e);
            entity.setStatus(OrderStatus.REJECTED.name());
        }

        orderRepository.save(entity);
        return mapToOrder(entity);
    }

    @Override
    public Order modifyOrder(Order order) {
        // Find existing, apply changes, track modification count
        return null;
    }

    @Override
    public void cancelOrder(String brokerOrderId) {
        kiteApiClient.cancelOrder(brokerOrderId);
    }

    @Override
    public void reconcilePendingOrders() {
        // Fetch AMO_PENDING or OPEN orders and sync with Kite's order book
        List<String> activeStates = Arrays.asList(OrderStatus.AMO_PENDING.name(), OrderStatus.OPEN.name(), OrderStatus.PARTIAL.name());
        List<OrderEntity> pendingEntities = orderRepository.findByStatusIn(activeStates);
        
        if (pendingEntities.isEmpty()) return;
        
        log.info("Reconciling {} active orders with broker order book", pendingEntities.size());
        Map<String, String> brokerOrderBook = kiteApiClient.fetchOrderBookStatus();
        
        for (OrderEntity entity : pendingEntities) {
            String brokerId = entity.getBrokerOrderId();
            if (brokerId != null && brokerOrderBook.containsKey(brokerId)) {
                String brokerStatus = brokerOrderBook.get(brokerId);
                
                // AMO_PENDING specific transition check
                if (entity.getStatus().equals(OrderStatus.AMO_PENDING.name()) && brokerStatus.equals("OPEN")) {
                    log.info("AMO Order {} transitioned to OPEN", brokerId);
                    entity.setStatus(OrderStatus.OPEN.name());
                    orderRepository.save(entity);
                } else if (!entity.getStatus().equals(brokerStatus)) {
                    log.info("Order status mismatch for {}. DB: {}, Broker: {}", brokerId, entity.getStatus(), brokerStatus);
                    entity.setStatus(brokerStatus);
                    orderRepository.save(entity);
                }
            } else if (entity.getStatus().equals(OrderStatus.AMO_PENDING.name()) && !isAmoWindow()) {
                // Orphaned AMO order from yesterday that didn't execute
                log.warn("Orphaned AMO order found: {}. Cancelling locally.", entity.getInternalOrderId());
                entity.setStatus(OrderStatus.CANCELLED.name());
                orderRepository.save(entity);
            }
        }
    }

    private boolean isAmoWindow() {
        // True if time is between 15:45 and 08:57 IST next day. Stubbed for now.
        return false;
    }

    private Order mapToOrder(OrderEntity entity) {
        Order o = new Order();
        o.setInternalOrderId(entity.getInternalOrderId());
        o.setStatus(OrderStatus.valueOf(entity.getStatus()));
        // ... map other fields
        return o;
    }
}
