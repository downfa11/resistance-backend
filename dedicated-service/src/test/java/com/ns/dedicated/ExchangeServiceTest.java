package com.ns.dedicated;

import com.ns.dedicated.application.service.ExchangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.ns.dedicated.application.service.ExchangeService.EXCHANGE_KEY;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ExchangeServiceTest {

    @InjectMocks private ExchangeService exchangeService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private HashOperations<String, String, String> exchangeOperations;


    @Test
    void 데이터가_빈_경우_재화에_대한_환율비_초기화_메서드() {
        // given
        when(redisTemplate.hasKey(EXCHANGE_KEY)).thenReturn(false);

        // when
        exchangeService.initExchangeRates();

        // then
        verify(exchangeOperations, times(1)).put(eq(EXCHANGE_KEY), anyString(), anyString());
        verify(exchangeOperations, times(1)).put(eq(ExchangeService.USAGE_KEY), anyString(), eq("0"));
    }

    @Test
    void 재화에_대한_환율비_초기화_메서드() {
        // given
        when(redisTemplate.hasKey(EXCHANGE_KEY)).thenReturn(true);

        // when
        exchangeService.initExchangeRates();

        // then
        verify(exchangeOperations, times(0)).put(eq(EXCHANGE_KEY), anyString(), anyString());
    }

    @Test
    void 모든_재화의_환율비를_조회하는_메서드() {
        // given
        Map<String, String> mockExchangeRates = new HashMap<>();
        mockExchangeRates.put("XAR", "100");
        mockExchangeRates.put("CIR", "120");

        when(exchangeOperations.get(EXCHANGE_KEY, "XAR")).thenReturn("100");
        when(exchangeOperations.get(EXCHANGE_KEY, "CIR")).thenReturn("120");

        // when
        Map<String, Integer> result = exchangeService.getExchangeRates();

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100, result.get("XAR"));
        assertEquals(120, result.get("CIR"));
    }

    @Test
    void 특정_재화의_환율을_조정하는_메서드() {
        // given
        Map<String, Integer> mockUsageData = new HashMap<>();
        mockUsageData.put("XAR", 10);
        mockUsageData.put("CIR", 5);

        Map<String, Integer> mockRates = new HashMap<>();
        mockRates.put("XAR", 100);
        mockRates.put("CIR", 120);

        when(exchangeService.getUsageData()).thenReturn(mockUsageData);
        when(exchangeService.getExchangeRates()).thenReturn(mockRates);

        // when
        exchangeService.adjustExchangeRates();

        // Then
        verify(exchangeOperations, times(2)).put(eq(EXCHANGE_KEY), anyString(), anyString());
    }

    @Test
    void 특정_재화의_환율을_설정하는_메서드() {
        // given
        String currency = "XAR";

        // when
        exchangeService.setExchangeRates(currency);

        // Then
        verify(exchangeOperations, times(1)).increment(eq(ExchangeService.USAGE_KEY), eq(currency), eq(1L));
    }

    @Test
    void 재화의_전체_사용량을_조회하는_메서드() {
        // given
        Map<String, Integer> mockUsageData = new HashMap<>();
        mockUsageData.put("XAR", 10);
        mockUsageData.put("CIR", 5);

        when(exchangeService.getUsageData()).thenReturn(mockUsageData);

        // when
        Integer totalUsage = exchangeService.calculateTotalUsage();

        // Then
        assertEquals(15, totalUsage);
    }

    @Test
    void 데이터가_없는데_재화의_전체_사용량을_조회하는_메서드() {
        // given
        Map<String, Integer> mockUsageData = new HashMap<>();
        when(exchangeService.getUsageData()).thenReturn(mockUsageData);

        // when
        Integer totalUsage = exchangeService.calculateTotalUsage();

        // Then
        assertEquals(1, totalUsage);
    }
}
