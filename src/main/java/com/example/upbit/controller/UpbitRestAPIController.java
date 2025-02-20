package com.example.upbit.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.service.CoinTradeService;
import com.example.upbit.service.GetCoinService;
import com.example.upbit.service.event.StopTradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
@Slf4j
public class UpbitRestAPIController {
    private final GetCoinService getCoinService;
    private final CoinTradeService coinTradeService;
    private final StopTradeService stopTradeService;

    @GetMapping("/api/get/filteringCoin")
    public String getFilteringCoin() {
        return getCoinService.findBuyTargets().toString();
    }
    
    @GetMapping("/api/do/trade")
    public void doTrade(@RequestParam(name="price", required = false) final String startPrice) {
        coinTradeService.tryTrade(startPrice);
    }

    @GetMapping("/stop/trade")
    public String stopTrade() {
        return "Trading Status is " + stopTradeService.stopTrade() + "";
    }
    
    
}
