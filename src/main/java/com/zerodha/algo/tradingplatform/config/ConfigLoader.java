package com.zerodha.algo.tradingplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConfigLoader {

    private final Validator validator;

    @Value("${trading.config.path:strategies.yml}")
    private String configPath;

    @Bean
    public TradingConfig loadTradingConfig() {
        log.info("Loading strategies configuration from {}", configPath);
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            File file = new File(configPath);
            if (!file.exists()) {
                throw new IllegalStateException("Strategy config file not found: " + configPath);
            }

            TradingConfig config = mapper.readValue(file, TradingConfig.class);

            Set<ConstraintViolation<TradingConfig>> violations = validator.validate(config);
            if (!violations.isEmpty()) {
                log.error("Configuration constraint violations found in {}:", configPath);
                for (ConstraintViolation<TradingConfig> violation : violations) {
                    log.error("  {} {}", violation.getPropertyPath(), violation.getMessage());
                }
                throw new ConstraintViolationException(violations);
            }
            
            log.info("Successfully loaded and validated {} strategies.", config.getStrategies().size());
            return config;
        } catch (ConstraintViolationException e) {
            throw e; // Fail fast on validation
        } catch (Exception e) {
            log.error("Failed to parse config file at startup", e);
            throw new IllegalStateException("Failed to parse config: " + e.getMessage(), e);
        }
    }
}
