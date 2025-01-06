package com.example.upbit.service;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UpbitRestApiService {

    // 1초의 시장가를 가져오는 API EndPoint
    private static final String API_URL = "https://api.upbit.com/v1/candles/seconds";
    private static final String MARKET = "KRW-BTC"; // 예시: 비트코인
    private static final double PURCHASE_PRICE = 50000000; // 매수 가격 예시 (50,000,000원)
    private static final double SELL_CONDITION = 0.003; // 0.3% 상승 조건
    private static final double STOP_LOSS = -0.01; // -1% 손절 조건

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new ParameterizedTypeReference<List<Map<String,Object>>>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<Map<String,Object>>() {};

    private final ObjectMapper objectMapper;

    private final WebClient webClient;

    public void startTrading() {

        int count = 0;

        // 1초마다 시장가를 확인하고 매도 조건에 맞으면 매도하는 로직+
        while (count < 40) {
            //System.out.println(getOnePrice());
            checkMarketPriceAndTrade();
            try {
                Thread.sleep(1000); // 1초 대기
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Double getOnePrice(){
        ResponseEntity<List<Map<String,Object>>> responseBody =  webClient
                    .get()
                    .uri(API_URL + "?market=" + MARKET + "&count=1")
                    .retrieve()
                    .toEntity(LIST_MAP_TYPE)
                    .block();
        

        Double response = (Double) responseBody.getBody().get(0).get("opening_price");
    
        return response;
    }

    private void checkMarketPriceAndTrade() {
        getMarketPrice()
                //.doOnNext(this::evaluateSellCondition)
                .subscribe();
    }

    private Mono<Long> getMarketPrice() {
        return webClient.get()
            .uri(API_URL + "?market=" + MARKET + "&count=1")
            .exchangeToMono(response -> response.bodyToMono(LIST_MAP_TYPE))
            .map(data -> parseMarketPrice(data))
            .doOnNext(data -> System.out.println("Value : " + data));
    }

    private Long parseMarketPrice(List<Map<String, Object>> jsonResponse) {
        // 응답에서 현재 시장가 추출 (여기서는 예시로 JSON 파싱하는 방법 사용)
        // 실제로는 JSON 라이브러리로 응답을 파싱하여 가격을 가져옵니다.

        double price = (double) jsonResponse.get(0).get("opening_price");

        return (long)price; // 예시로 50,500,000원이라고 가정
    }

    private void evaluateSellCondition(Double currentPrice) {
        double priceDifference = (currentPrice - PURCHASE_PRICE) / PURCHASE_PRICE;

        if (priceDifference >= SELL_CONDITION) {
            sellMarketOrder(currentPrice);
        } else if (priceDifference <= STOP_LOSS) {
            sellMarketOrder(currentPrice);
        }
    }

    private void sellMarketOrder(Double currentPrice) {
        // 시장가로 매도하는 API 호출 (실제 매도 로직)
        System.out.println("Selling at price: " + currentPrice);
        // 실제로는 매도 API를 호출해야 합니다.
    }
}
