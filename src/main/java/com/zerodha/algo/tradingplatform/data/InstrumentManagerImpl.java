package com.zerodha.algo.tradingplatform.data;

import com.zerodha.algo.tradingplatform.core.InstrumentManager;
import com.zerodha.algo.tradingplatform.model.Instrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentManagerImpl implements InstrumentManager {

    private static final String KITE_INSTRUMENTS_URL = "https://api.kite.trade/instruments";
    
    private final Map<String, Instrument> instrumentCache = new ConcurrentHashMap<>();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final DateTimeFormatter expiryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Scheduled(cron = "0 30 8 * * *", zone = "Asia/Kolkata")
    public void downloadAndParseInstruments() {
        log.info("Starting daily instrument master download from {}", KITE_INSTRUMENTS_URL);
        Request request = new Request.Builder()
                .url(KITE_INSTRUMENTS_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to download instruments CSV. HTTP {}", response.code());
                return;
            }

            String csvBody = response.body().string();
            parseCsv(csvBody);
            
            log.info("Successfully loaded {} instruments into master cache.", instrumentCache.size());
        } catch (Exception e) {
            log.error("Error downloading/parsing instrument master", e);
        }
    }

    private void parseCsv(String csvData) {
        try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 12) continue;
                
                // CSV Format: instrument_token,exchange_token,tradingsymbol,name,last_price,expiry,strike,tick_size,lot_size,instrument_type,segment,exchange
                try {
                    Instrument instrument = new Instrument();
                    instrument.setInstrumentToken(parts[0]);
                    instrument.setExchangeToken(parts[1]);
                    instrument.setTradingsymbol(parts[2]);
                    instrument.setName(parts[3]);
                    instrument.setLastPrice(new BigDecimal(parts[4]));
                    
                    if (!parts[5].isEmpty()) {
                        instrument.setExpiry(LocalDate.parse(parts[5], expiryFormatter));
                    }
                    
                    instrument.setStrike(parts[6]);
                    instrument.setTickSize(new BigDecimal(parts[7]));
                    instrument.setLotSize(Integer.parseInt(parts[8]));
                    instrument.setInstrumentType(parts[9]);
                    instrument.setSegment(parts[10]);
                    instrument.setExchange(parts[11]);
                    
                    instrumentCache.put(instrument.getInstrumentToken(), instrument);
                } catch (Exception parseEx) {
                    // Ignore malformed rows safely
                }
            }
        } catch (Exception e) {
            log.error("Error reading CSV payload", e);
        }
    }

    @Override
    public Instrument getInstrumentByToken(String instrumentToken) {
        return instrumentCache.get(instrumentToken);
    }

    @Override
    public Map<String, Instrument> getAllInstruments() {
        return instrumentCache;
    }
}
