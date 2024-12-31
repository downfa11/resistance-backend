package com.ns.dedicated.domain;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CurrencyRate {
    JPY(1090),
    XAG(27),
    XPT(1201),
    CNY(177),
    SPD(1),
    KRW(1335);

    private final int defaultRate;

    public int getDefaultRate() {
        return defaultRate;
    }

    public static Map<String, Integer> getDefaultExchangeRates() {
        Map<String, Integer> rates = new HashMap<>();
        for (CurrencyRate currency : values()) {
            rates.put(currency.name(), currency.getDefaultRate());
        }
        return rates;
    }
}
