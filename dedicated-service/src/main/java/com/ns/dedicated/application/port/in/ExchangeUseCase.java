package com.ns.dedicated.application.port.in;

import java.util.Map;

public interface ExchangeUseCase {
    Map<String,Integer> getExchangeRates();

    void initExchangeRates();

    void adjustExchangeRates();

    void setExchangeRates(String money);

}
