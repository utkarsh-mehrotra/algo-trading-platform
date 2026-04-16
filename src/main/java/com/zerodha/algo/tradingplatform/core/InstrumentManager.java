package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.model.Instrument;
import java.util.Map;

public interface InstrumentManager {
    void downloadAndParseInstruments();
    Instrument getInstrumentByToken(String instrumentToken);
    Map<String, Instrument> getAllInstruments();
}
