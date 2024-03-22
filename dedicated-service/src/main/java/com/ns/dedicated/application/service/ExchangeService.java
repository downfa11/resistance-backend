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

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> exchangeOperations;

    public ExchangeService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.exchangeOperations = redisTemplate.opsForHash();
    }

    @Override
    public void initExchangeRates() {
        Map<String, Integer> exchangeData = new HashMap<>();
        exchangeData.put("JPY", 1090);
        exchangeData.put("XAG", 27);
        exchangeData.put("XPT", 1201);
        exchangeData.put("CNY", 177);
        exchangeData.put("SPD", 1);
        exchangeData.put("KRW", 1335);

        exchangeData.forEach((currency, rate) -> {
            exchangeOperations.put("exchangeRates", currency, String.valueOf(rate));
            exchangeOperations.put("usage", currency, "0");
        });
    }

    @Override
    public Map<String, Integer> getExchangeRates() {
        Map<String, Integer> exchangeData = new HashMap<>();
        exchangeData.put("JPY", Integer.parseInt(exchangeOperations.get("exchangeRates", "JPY")));
        exchangeData.put("XAG", Integer.parseInt(exchangeOperations.get("exchangeRates", "XAG")));
        exchangeData.put("XPT", Integer.parseInt(exchangeOperations.get("exchangeRates", "XPT")));
        exchangeData.put("CNY", Integer.parseInt(exchangeOperations.get("exchangeRates", "CNY")));
        exchangeData.put("SPD", Integer.parseInt(exchangeOperations.get("exchangeRates", "SPD")));
        exchangeData.put("KRW", Integer.parseInt(exchangeOperations.get("exchangeRates", "KRW")));
        return exchangeData;
    }

    @Override
    public void adjustExchangeRates() {
        Map<String, Integer> exchangeData = getExchangeRates();
        double average = calculateAverageExchangeAttempt();
        exchangeData.forEach((currency, rate) -> {
            int newRate = rate < average ? (int)(rate * 1.01) : (int)(rate * 0.99);
            exchangeOperations.put("exchangeRates", currency, String.valueOf(newRate));
        });
    }

    @Override
    public void setExchangeRates(String money) {
        // 해당 재화의 사용량을 1 증가
        exchangeOperations.increment("usage", money, 1);
    }

    private double calculateAverageExchangeAttempt() {
        Map<String, Integer> exchangeData = getExchangeRates();
        double total = exchangeData.values().stream().mapToDouble(Integer::doubleValue).sum();
        return total / exchangeData.size();
    }
}