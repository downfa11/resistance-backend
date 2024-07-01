package com.ns.dedicated.adpater.in.web;

import com.ns.dedicated.adpater.in.web.dto.ExchangeRateResponse;
import com.ns.dedicated.adpater.out.JwtTokenProvider;
import com.ns.dedicated.application.port.in.ExchangeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exchange")
public class ExchangeController {
    private final ExchangeUseCase exchangeUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/rates")
    public ExchangeRateResponse getExchangeRates(){

        Long memberId = jwtTokenProvider.getMembershipIdbyToken();
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .exchangeRates(exchangeUseCase.getExchangeRates()).build();
        return response;
    }

    @GetMapping("init")
    public ExchangeRateResponse initExchangeRates() { exchangeUseCase.initExchangeRates();
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .exchangeRates(exchangeUseCase.getExchangeRates()).build();
        return response;
    }

    @GetMapping("/update")
    public ExchangeRateResponse updateExchangeRates(){
        exchangeUseCase.adjustExchangeRates();
        // Todo. 일정 시간마다 호출해주고, 현재 시간과 함께 Log를 남겨두면 나중에 시각화하기도 좋다.
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .exchangeRates(exchangeUseCase.getExchangeRates()).build();
        return response;
    }

    @PostMapping("/set/{money}")
    public ExchangeRateResponse setExchangeRates(@PathVariable String money){

        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        exchangeUseCase.setExchangeRates(money);
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .exchangeRates(exchangeUseCase.getExchangeRates()).build();
        return response;
    }
}
