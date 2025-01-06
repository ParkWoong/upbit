package com.example.upbit.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static com.example.upbit.config.WebClientConfig.getSend;
//import static com.example.upbit.config.WebClientConfig.postSend;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpbitService {
    private final WebClient webClient;

    private final Set<String> activeTrades = new HashSet<>();

    /*
     * 1분봉 호출
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOneMinuteCandles(String market){
        
        final String path = "/candles/minutes/1";


        return (List<Map<String, Object>>) getSend(path, 
                null, 
                null, 
                new ParameterizedTypeReference<List<Map<String, String>>>(){})
                .getBody();
    }

    /*
     * 마켓 리스트 호출
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getMarkets(){
        
        final String path = "/market/all";


        return (List<Map<String, String>>) getSend(path, 
                null, 
                null, 
                new ParameterizedTypeReference<List<Map<String, String>>>(){})
                .getBody();
    }

    /*
     * 상승 거래량 이 큰 두 코인
     */
        public List<String> findTopTwoRisingCoins() {
        List<Map<String, String>> markets = getMarkets();
        List<Map<String, Object>> volumeList = new ArrayList<>();

        for (Map<String, String> market : markets) {
           
            String marketCode = market.get("market");
           
            List<Map<String, Object>> candles = getOneMinuteCandles(marketCode);

            if (!(candles == null || candles.isEmpty())) {
                Map<String, Object> candle = candles.get(0);
                candle.put("market", marketCode);
                volumeList.add(candle);
            }
        }

        // 거래량 기준으로 정렬
        volumeList.sort((a, b) -> {
            Double volumeA = (Double) a.get("candle_acc_trade_volume");
            Double volumeB = (Double) b.get("candle_acc_trade_volume");
            return volumeB.compareTo(volumeA);
        });

        // 상위 두 개의 마켓 반환
        List<String> topTwoCoins = new ArrayList<>();
        for (int i = 0; i < 2 && i < volumeList.size(); i++) {
            topTwoCoins.add((String) volumeList.get(i).get("market"));
        }

        return topTwoCoins;
    }
}
