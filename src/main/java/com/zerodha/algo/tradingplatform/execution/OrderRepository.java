package com.zerodha.algo.tradingplatform.execution;

import com.zerodha.algo.tradingplatform.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByStatusIn(List<String> statuses);
    List<OrderEntity> findByStatusAndCreatedAtAfter(String status, java.time.Instant date);
}
