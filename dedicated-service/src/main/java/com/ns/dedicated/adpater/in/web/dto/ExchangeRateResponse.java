package com.ns.dedicated.adpater.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExchangeRateResponse {
    private Map<String, Integer> exchangeRates;
}
