package com.ns.dedicated.adpater.in.web;

import com.ns.dedicated.application.port.in.ExchangeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exchange")
public class ExchangeController {
    private final ExchangeUseCase exchangeUseCase;

    @GetMapping("/rates")
    public Map<String,Integer> getExchangeRates(){
        return exchangeUseCase.getExchangeRates();
    }

    @GetMapping("init")
    public Map<String,Integer> initExchangeRates() { exchangeUseCase.initExchangeRates(); return exchangeUseCase.getExchangeRates();}

    @GetMapping("/update")
    public Map<String, Integer> updateExchangeRates(){
        exchangeUseCase.adjustExchangeRates();
        // Todo. 일정 시간마다 호출해주고, 현재 시간과 함께 Log를 남겨두면 나중에 시각화하기도 좋다.
        return exchangeUseCase.getExchangeRates();
    }

    @PostMapping("/set/{money}")
    public Map<String, Integer> setExchangeRates(@PathVariable String money){
        exchangeUseCase.setExchangeRates(money);
        return exchangeUseCase.getExchangeRates();
    }
}
