package com.zerodha.algo.tradingplatform.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zerodha.algo.tradingplatform.core.MarketDataService;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMarketDataServiceImpl implements MarketDataService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void subscribe(String instrumentToken) {
        // Calls Kite Connect SDK or underlying WebSocket to subscribe
        log.info("Subscribing to instrument token: {}", instrumentToken);
    }

    @Override
    public void unsubscribe(String instrumentToken) {
        // Calls Kite Connect SDK or underlying WebSocket to unsubscribe
        log.info("Unsubscribing from instrument token: {}", instrumentToken);
    }

    @Override
    public List<Candlestick> getHistoricalBars(String instrumentToken, String timeframe, int limit) {
        String key = "bars:" + instrumentToken + ":" + timeframe;
        // Retrieve the top N bars from ZSET using reverse range
        Set<String> jsonBars = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        if (jsonBars == null) {
            return Collections.emptyList();
        }
        
        List<Candlestick> bars = jsonBars.stream()
                .map(this::parseCandlestick)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // Reverse again to return in chronological order
        Collections.reverse(bars);
        return bars;
    }
    
    public void saveCompletedBar(Candlestick bar) {
        String key = "bars:" + bar.getInstrumentToken() + ":" + bar.getTimeframe();
        try {
            String json = mapper.writeValueAsString(bar);
            // Score by epoch seconds to keep it sorted in ZSET
            double score = bar.getStartTime().getEpochSecond();
            redisTemplate.opsForZSet().add(key, json, score);
            
            // Keep only the last 1000 bars per instrument / timeframe as per requirements
            redisTemplate.opsForZSet().removeRange(key, 0, -1001);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bar", e);
        }
    }

    private Candlestick parseCandlestick(String json) {
        try {
            return mapper.readValue(json, Candlestick.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Candlestick from Redis", e);
            return null;
        }
    }
}
