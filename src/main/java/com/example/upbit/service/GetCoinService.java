package com.example.upbit.service;



import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetCoinService {

    private final WebClient webClient;
    private final static String API_URL = "https://api.upbit.com/v1";
    public static List<String> collectedCoins = new ArrayList<>();


    protected void getCoin(){
        collectedCoins = findBuyTargets();
    }

    /**
     * 매수 대상을 탐색하는 메서드
     */
    public List<String> findBuyTargets() {
        // 1. 전체 마켓 정보 가져오기
        List<String> allMarkets = getAllMarkets();
        log.info("1. Get All Coins : {}, count : {}", allMarkets.toString(), allMarkets.size());

        // 2. 거래량 급증한 코인 필터링
        List<String> highVolumeMarkets = findHighVolumeMarkets(allMarkets);
        log.info("2. Filtering High Trading Coins : {}, count : {}", highVolumeMarkets.toString(), highVolumeMarkets.size());

        // 3. RSI 값이 30 이상인 코인 필터링
        List<String> rsiAbove30Markets = filterMarketsByRSI(highVolumeMarkets);
        log.info("3. Filtering over RSI > 50 Coins : {}, count : {}", rsiAbove30Markets.toString(), rsiAbove30Markets.size());

        // 4. 매수 주문량 > 매도 주문량 필터링
        List<String> coinList = filterMarketsByOrderBook(rsiAbove30Markets);
        log.info("4. Filtering total trading BIZ : {}, count : {}", coinList.toString(), coinList.size());

        return coinList;
    }

    /**
     * 전체 마켓 정보를 가져오는 메서드
     */
    @SuppressWarnings("all")
    public List<String> getAllMarkets() {
        List<String> allCoins = (List<String>) Objects.requireNonNull(webClient.get()
                .uri(API_URL + "/market/all")
                .retrieve()
                .bodyToMono(List.class)
                .block())
                .stream()
                .map(market -> (String) ((Map<String, Object>) market).get("market"))
                .filter(market -> ((String) market).startsWith("KRW-")) // KRW 마켓만 가져오기
                //.limit(2) // 앞의 코인 3개만
                .collect(Collectors.toList());
        return allCoins;
    }

    /**
     * 거래량이 급증한 코인을 필터링하는 메서드
     */
    private List<String> findHighVolumeMarkets(List<String> allMarkets) {
        List<String> highVolumeMarkets = new ArrayList<>();
        allMarkets.forEach(market -> {
            log.info("Market : {}", market);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Map<String, Object> tickerData = getTickerData(market);
            if (tickerData != null) {
                
                //log.info("TickerData : {}", tickerData.get("acc_trade_volume_24h"));
                
                double tradeVolume = (double) tickerData.get("acc_trade_volume_24h");
                double avgTradeVolume = calculateAverageTradeVolume(market);

                // 최근 거래량이 평균 대비 2배 이상 증가한 경우
                if (tradeVolume > avgTradeVolume * 2) {
                    highVolumeMarkets.add(market);
                }
            }
        });

        //log.info("High Trading Coins : {}", highVolumeMarkets.toString());

        return highVolumeMarkets;
    }

    /**
     * 특정 코인의 평균 거래량 계산
     */
    private double calculateAverageTradeVolume(String market) {
        // 이 함수는 최근 n개의 데이터를 가져와 평균 거래량을 계산합니다.
        List<Map<String, Object>> candles = getRecentCandles(market, 5); // 최근 5분 캔들 가져오기
        return candles.stream()
                .mapToDouble(candle -> (double) candle.get("candle_acc_trade_volume"))
                .average()
                .orElse(0.0);
    }

    /**
     * RSI 값이 30 이상인 코인을 필터링하는 메서드
     */
    private List<String> filterMarketsByRSI(List<String> markets) {
        return markets.stream()
                .filter(market -> {
                    List<Double> closingPrices = getClosingPrices(market, 14); // 최근 14개의 종가 가져오기
                    double rsi = calculateRSI(closingPrices);
                    return rsi >= 50;
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 코인의 매수 주문량 > 매도 주문량 필터링
     */
    private List<String> filterMarketsByOrderBook(List<String> markets) {
        return markets.stream()
                .map(market -> {
                    Map<String, Object> orderBook = getOrderBookData(market);
                    if (orderBook != null) {
                        double totalBidSize = (double) orderBook.get("total_bid_size"); // 호가 매수 총잔량
                        double totalAskSize = (double) orderBook.get("total_ask_size"); // 호가 매도 총잔량
                        final double bizDiff = totalBidSize - totalAskSize;

                        return new AbstractMap.SimpleEntry<>(market, bizDiff);
                    }
                    return null;
                    }
                )
                .filter(Objects::nonNull)
                .filter(data -> data.getValue() > 0)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 매수량 차이 기준 내림차순 정렬
                .limit(1) // 1개의 코인만 가져옴옴
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    //=======================================
    // 특정 코인의 종가 리스트 가져오기
    //=======================================
    private List<Double> getClosingPrices(String market, int count) {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> candles = getRecentCandles(market, count);
        return candles.stream()
                .map(candle -> (double) candle.get("trade_price"))
                .collect(Collectors.toList());
    }

    /**
     * RSI 계산
     */
    private double calculateRSI(List<Double> closingPrices) {
        if (closingPrices.size() < 14) return 0.0;

        double gain = 0.0, loss = 0.0;
        for (int i = 1; i < closingPrices.size(); i++) {
            double change = closingPrices.get(i) - closingPrices.get(i - 1);
            if (change > 0) gain += change;
            else loss -= change;
        }
        double avgGain = gain / 14;
        double avgLoss = loss / 14;

        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * RestAPI로 특정 마켓의 캔들 데이터 가져오기
     */
    @SuppressWarnings("all")
    private List<Map<String, Object>> getRecentCandles(String market, int count) {
        return Objects.requireNonNull(webClient.get()
                .uri(API_URL, uriBuilder -> uriBuilder
                        .path("/candles/minutes/1") // 1분 캔들
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block());
    }

    /**
     * 특정 마켓의 실시간 티커 데이터 가져오기
     */
    @SuppressWarnings("all")
    public Map<String, Object> getTickerData(String market) {
        Map<String, Object> result = fetchTickerData(market);

        if (result == null) {
            System.out.println("getTickerData() 반환값이 null, 다시 호출...");
            return getTickerData(market); // 재귀 호출
        }
        return result;
    }


    @SuppressWarnings("all")
    public Map<String, Object> fetchTickerData(String market) {

        return (Map<String, Object>) webClient.get()
                .uri(API_URL, uriBuilder -> uriBuilder
                        .path("/ticker")
                        .queryParam("markets", market)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block()
                .stream()
                .map(data -> (Map<String, Object>) data)
                .findFirst()
                .orElse(null);
    }

    /**
     * 특정 마켓의 주문 데이터 가져오기
     */
    @SuppressWarnings("all")
    private Map<String, Object> getOrderBookData(String market) {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return (Map<String, Object>) Objects.requireNonNull(webClient.get()
                .uri(API_URL, uriBuilder -> uriBuilder
                        .path("/orderbook")
                        .queryParam("markets", market)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block())
                .stream()
                .map(data -> (Map<String, Object>) data)
                .findFirst()
                .orElse(null);
    }
}
