package com.ns.dedicated.adpater.in.web.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExchangeRateResponse {
    private Map<String, Integer> exchangeRates;
}
