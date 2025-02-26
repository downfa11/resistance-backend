package com.ns.dedicated.application.service;


import com.ns.common.UseCase;
import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.dedicated.adpater.out.persistance.BoardMapper;
import com.ns.dedicated.application.port.in.ExchangeUseCase;
import com.ns.dedicated.application.port.in.FindBoardUseCase;
import com.ns.dedicated.application.port.in.ModifyBoardUseCase;
import com.ns.dedicated.application.port.in.RegisterBoardUseCase;
import com.ns.dedicated.application.port.in.command.FindBoardCommand;
import com.ns.dedicated.application.port.in.command.ModifyBoardCommand;
import com.ns.dedicated.application.port.in.command.RegisterBoardCommand;
import com.ns.dedicated.application.port.out.FindBoardPort;
import com.ns.dedicated.application.port.out.ModifyBoardPort;
import com.ns.dedicated.application.port.out.RegisterBoardPort;
import com.ns.dedicated.domain.Board;
import com.ns.dedicated.domain.CurrencyRate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@UseCase
public class ExchangeService implements ExchangeUseCase {

    public static final String EXCHANGE_KEY = "exchangeRates";
    public static final String USAGE_KEY = "usage";
    private static final Double BASE_RATE = 0.95;
    private static final Double RELATIVE_RATE_FACTOR = 0.1;

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> exchangeOperations;

    public ExchangeService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.exchangeOperations = redisTemplate.opsForHash();
    }

    @Override
    public void initExchangeRates() {
        if(redisTemplate.hasKey(EXCHANGE_KEY))
            return;

        CurrencyRate.getDefaultExchangeRates().forEach((currency, rate) -> {
            exchangeOperations.put(EXCHANGE_KEY, currency, String.valueOf(rate));
            exchangeOperations.put(USAGE_KEY, currency, "0");
        });
    }


    @Override
    public Map<String, Integer> getExchangeRates() {
        Map<String, Integer> exchangeData = new HashMap<>();

        CurrencyRate.getDefaultExchangeRates().keySet().forEach(currency -> {
            String rate = exchangeOperations.get(EXCHANGE_KEY, currency);
            exchangeData.put(currency, Integer.parseInt(rate));
        });

        return exchangeData;
    }


    @Override
    public void adjustExchangeRates() {
        Map<String, Integer> usageData = getUsageData();
        int totalUsage = calculateTotalUsage();

        getExchangeRates().forEach((currency, rate) -> {
            int usage = usageData.getOrDefault(currency, 0);
            exchangeOperations.put(EXCHANGE_KEY, currency, String.valueOf(calcNewRate(rate,totalUsage, usage)));
        });
    }

    private double calcNewRate(Integer rate, Integer totalUsage, Integer usage){
        double relative = 1 - (double) usage / totalUsage;
        double adjustRate = BASE_RATE + (relative * RELATIVE_RATE_FACTOR);

        return rate * adjustRate;
    }

    public Map<String, Integer> getUsageData() {
        Map<String, String> usageDataToString = exchangeOperations.entries(USAGE_KEY);
        Map<String, Integer> usageDataToInteger = new HashMap<>();

        usageDataToString.forEach((key, value) -> {
            try {
                usageDataToInteger.put(key, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                usageDataToInteger.put(key, 0);
            }
        });

        return usageDataToInteger;
    }

    @Override
    public void setExchangeRates(String money) {
        // 해당 재화의 사용량을 1 증가
        exchangeOperations.increment(USAGE_KEY, money, 1);
    }

    public Integer calculateTotalUsage() {
        Integer totalUsage = getUsageData().values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        // 분모가 0이 될 수 없어
        return totalUsage == 0 ? 1 : totalUsage;
    }

}