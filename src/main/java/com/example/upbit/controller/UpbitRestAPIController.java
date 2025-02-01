package com.example.upbit.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.service.GetCoinService;
import com.example.upbit.service.UpbitRestApiService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;




@RestController
@RequiredArgsConstructor
public class UpbitRestAPIController {
    
    private final UpbitRestApiService service;
    private final GetCoinService getCoinService;

    @GetMapping("/api/get/tradingInfo")
    public void getAPITradingInfo() {

        //String MARKET = "KRW-BONK";

        service.getCoin();
    }

    // @GetMapping("/api/current/tradingInfo")
    // public String getCurrentInfo() {
    //     return service.getOnePrice();
    // }

    @GetMapping("/api/get/filteringCoin")
    public String getFilteringCoin() {
        return getCoinService.findBuyTargets().toString();
    }
    
    
}
