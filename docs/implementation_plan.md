# Algorithmic Trading Platform for Indian Equity Markets

This plan details the architecture and implementation steps for building a production-grade, low-latency (≤ 300ms p99) multi-strategy algorithmic trading system targeting the NSE via Zerodha's Kite Connect v3 API.

## Architecture & Technology Stack

- **Core**: Java 21 (Virtual Threads for I/O), Spring Boot 3.2.x
- **Build Tool**: Gradle (Kotlin DSL) for strong dependency management and multi-module support (if needed, though we operate as a modular monolith).
- **Database**: PostgreSQL 15 (Order states, historical trades), Redis 7 (Market data, completed bars, fast in-memory lookups).
- **Integrations**: OkHttp 4 (Kite REST API), Java-WebSocket / Spring WebSocket (Kite Ticks), Micrometer + Prometheus (Metrics).

## Proposed Implementation Phases & Deliverables

### Phase 1: Project Scaffold & Core Models
- Set up a Spring Boot project with Gradle in `~/Documents/algo-trading-platform`.
- Define core interfaces (`TradingStrategy`, `ExecutionEngine`, `MarketDataService`).
- Define core domain models (`Tick`, `Candlestick`, `Order`, `OrderStatus`, `Signal`, `Instrument`).
- Setup the core exception hierarchy constraint and centralized logging (SLF4J + Logback with JSON layout).
- **Instrument Master**: Download Kite instruments CSV daily at 08:30 IST. Parse lot sizes, tick sizes, expiry dates, and index by `instrument_token`.

### Phase 2: Configuration Loader
- Implement a YAML config loader using `jackson-dataformat-yaml` and `jakarta.validation-api`.
- Parse global settings, strategy beans dynamically, and risk rules.
- Add fail-fast validation on application startup ensuring all constraints (like `warmup_bars`, `risk` objects) are strictly valid.

### Phase 3: Market Data & Aggregation
- **WebSocket Ingestion**: Connect to Kite WebSocket, ingest live ticks. **Deduplicate on `(instrument_token, exchange_timestamp)` tuple with a bounded LRU cache per instrument.**
- **Bar Aggregation**: Implement time-based aggregators (1m, 5m, 15m) aligned to NSE open (09:15 IST).
- **Indicators**: Compute EMA, SMA, RSI, and VWAP incrementally as bars complete.
- **Caching & Recovery**: Store completed bars to Redis (ZSET). Add startup hook to fetch missing historical bars via Kite's `historical` endpoint (handling the rate limits and API logic).

### Phase 4: Strategy Engine
- Implement a `StrategyManager` to instantiate and route events to strategies.
- Implement the `TradingStrategy` interface logic.
- Reference Implementations:
  - `EMACrossoverStrategy`: Momentum strategy.
  - `ShortStraddleStrategy`: Options strategy.
- Add schedule constraints (entry windows, hard exits).

### Phase 5: Execution Engine
- Full Order State Machine mapping `PENDING` -> `OPEN` -> `PARTIAL` -> `FILLED` -> `CLOSED`. Additionally, handle `AMO_PENDING` orders, and explicitly manage their transition to `OPEN` after the pre-market session.
- Persist order intent to PostgreSQL as 'Write-Ahead' before hitting Kite API.
- Reconcile pending orders against Kite API on startup to handle crashes. Leftover `AMO_PENDING` orders from the previous day that never opened will be handled here as a special case.
- Handle partial fills (`filledQty`, `totalQty`) and API retries via Resilience4j (Exponential backoff + jitter).

### Phase 6: Risk Management
- Implement fixed-fractional position sizing.
- Add global Portfolio Kill Switch (e.g., 5% daily loss).
- Add MIS Position hard exit trigger at 15:10 IST.
- Strategy-specific Risk checks (Greeks limits: Delta, Gamma, Theta, Vega). Note: Options greeks will be approximated using Black-Scholes calculations based on underlying LTP, strike, time to expiry, and implied volatility.

### Phase 7: Backtesting Harness
- **Data Source**: Bulk-download array chunks (up to 60 days) iteratively from Kite's `/instruments/historical` endpoint and cache locally for running the 2-year backtest.
- Feed ticks/bars into the strategy engine.
- Fill engine simulating next-bar open + slippage (0.1%).
- Limit order fill validation (`bar.low <= limitPrice` for buys).
- Metrics collector: Sharpe, Sortino, Max Drawdown, Win Rate.

### Phase 8 & 9: Observability & Deployment
- Expose `/actuator/prometheus`.
- Provide Grafana dashboard JSON (Latency, PnL, Active Orders, System Health).
- Setup `docker-compose.yml` for Postgres, Redis, Prometheus, Grafana, and the App.
- Provide a `github-actions.yml` for CI/CD checks and deployments.

## Verification Plan

### Automated Tests
- Unit tests for the YAML Loader to ensure fail-fast validation.
- Unit tests for the Candlestick Bar Aggregator edge cases (09:15 start, 15:30 end, gaps).
- Backtesting engine functional test against dummy data.
- **Latency SLA via Micrometer**: Add a Micrometer timer wrapping `signal -> placeOrder()` to measure signal-to-order latency. Set up a Grafana alert threshold on the p99 latency histogram to ensure it remains ≤ 300ms.

### Manual Verification
- Run docker-compose and verify metrics showing up in Grafana.
- Dry-run application startup to ensure schema generation and configuration parsing.
