package com.example.upbit.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.service.UpbitRestApiService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequiredArgsConstructor
public class UpbitRestAPIController {
    
    private final UpbitRestApiService service;
    
    @GetMapping("/api/get/tradingInfo")
    public void getAPITradingInfo() {
        service.startTrading();
    }

    // @GetMapping("/api/current/tradingInfo")
    // public String getCurrentInfo() {
    //     return service.getOnePrice();
    // }
    
}
