package com.zerodha.algo.tradingplatform.backtest;

import com.zerodha.algo.tradingplatform.core.InstrumentManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataDownloader {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final InstrumentManager instrumentManager;

    // Kite limits historical minute data to 60 days per request
    public void downloadTwoYearsData(String instrumentToken) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        log.info("Starting historical data bulk download for instrument {} from {} to {}", instrumentToken, start, end);

        // Fetch in 60-day chunks iteratively
        LocalDate chunkStart = start;
        while (chunkStart.isBefore(end)) {
            LocalDate chunkEnd = chunkStart.plusDays(59);
            if (chunkEnd.isAfter(end)) {
                chunkEnd = end;
            }

            fetchChunk(instrumentToken, chunkStart.format(dtf), chunkEnd.format(dtf));
            chunkStart = chunkEnd.plusDays(1);
            
            try {
                // Rate limits
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void fetchChunk(String token, String from, String to) {
        String url = String.format("https://api.kite.trade/instruments/historical/%s/minute?from=%s&to=%s", token, from, to);
        Request request = new Request.Builder()
                .url(url)
                // Assumes Auth headers are injected via OkHttp Interceptor defined elsewhere
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                // Append this JSON to a local cached CSV or DB for BacktestingEngine
                log.debug("Downloaded chunk {} to {} for {} successfully", from, to, token);
            } else {
                log.error("Failed chunk {} to {}. Code: {}", from, to, response.code());
            }
        } catch (Exception e) {
            log.error("Error downloading historical chunk", e);
        }
    }
}
