package com.example.upbit.service.api;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import com.example.upbit.properties.TradeKeyProperties;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UpbitRestApiService {

    private final GetCoinService getCoinService;
    // 1초의 시장가를 가져오는 API EndPoint
    private static final String API_URL = "https://api.upbit.com/v1/candles/seconds";
    
    private final TradeKeyProperties tradeKeyProperties;

    public static Map<String, Map<String, String>> currentCoinTradeMap = new ConcurrentHashMap<>();

    // 1개의 코인으로 거래 시작
    public void getCoin(){
        String coinName = getCoinService.findBuyTargets().get(0);

        String tradePrice = (String) getCoinService
                                            .getTickerData(coinName)
                                            .get(tradeKeyProperties.getTradePrice());
        
        Map<String, String> tradeInfo = new HashMap<>();
        
        tradeInfo.put(tradeKeyProperties.getTradePrice(), tradePrice);
        
        currentCoinTradeMap.put(coinName, tradeInfo);

        System.out.println(currentCoinTradeMap.toString());

    }

    
   
}
