package com.zerodha.algo.tradingplatform.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

class ConfigLoaderTest {

    private ConfigLoader configLoader;
    private Validator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = mock(Validator.class);
        configLoader = new ConfigLoader(validator);
    }

    @Test
    void testLoadTradingConfig_Success() throws IOException {
        Path configFile = tempDir.resolve("test-strategies.yml");
        String yaml = "strategies:\n" +
                "  - id: test_strategy\n" +
                "    class: TestStrategy\n" +
                "    instruments: [NIFTY50]\n" +
                "    timeframe: 5m\n" +
                "    warmup_bars: 26\n" +
                "    risk:\n" +
                "      max_risk_per_trade_pct: 1.0\n" +
                "      stop_loss_pct: 1.5\n" +
                "      target_pct: 3.0\n" +
                "    schedule:\n" +
                "      entry_window: \"09:20–14:30\"\n" +
                "      exit_by: \"15:10\"";
        Files.writeString(configFile, yaml);

        ReflectionTestUtils.setField(configLoader, "configPath", configFile.toString());
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        TradingConfig config = configLoader.loadTradingConfig();

        assertNotNull(config);
        assertEquals(1, config.getStrategies().size());
        assertEquals("test_strategy", config.getStrategies().get(0).getId());
    }

    @Test
    void testLoadTradingConfig_FileNotFound() {
        ReflectionTestUtils.setField(configLoader, "configPath", "non-existent.yml");
        assertThrows(IllegalStateException.class, () -> configLoader.loadTradingConfig());
    }
}
