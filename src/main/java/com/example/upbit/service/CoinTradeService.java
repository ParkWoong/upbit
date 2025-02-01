package com.example.upbit.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoinTradeService {

    private final String TRADE_ENDPOINT = "https://api.upbit.com/v1/order";
    
    public boolean tryTrade(final String coinName){

        // bid : 매수
        // ask : 매도
        // price : 100,000
        

        //======================

        

        return false;

    }

}
