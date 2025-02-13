package com.example.upbit.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@SuppressWarnings("all")
@Service
public class ShortTermTrendCoinService {
    private final WebClient webClient;

    /**
     * 1. 거래량 상위 10개 코인 가져오기
     */
    public List<String> getTopTradedCoins() {
        String url = "/ticker?markets=" + getAllMarkets();
        
        List<Map<String, Object>> response = webClient
                                                    .get()
                                                    .uri(url)
                                                    .retrieve()
                                                    .bodyToMono(List.class)
                                                    .block();

        return response.stream()
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("acc_trade_volume_24h"),
                        (Double) a.get("acc_trade_volume_24h")
                ))
                .limit(10)
                .map(data -> (String) data.get("market"))
                .collect(Collectors.toList());
    }

    /**
     * 2. 최근 5분 평균 거래량이 과거 30분 평균보다 2배 증가한 코인 찾기
     */
    public List<String> getHighVolumeCoins(List<String> topCoins) {
        return topCoins.stream()
                .filter(coin -> {
                    List<Map<String, Object>> candles = getCandleData(coin, "5", 7);
                    double recentAvg = candles.subList(0, 5).stream().mapToDouble(c -> (Double) c.get("candle_acc_trade_volume")).average().orElse(0);
                    double pastAvg = candles.subList(5, 7).stream().mapToDouble(c -> (Double) c.get("candle_acc_trade_volume")).average().orElse(0);
                    return recentAvg > pastAvg * 2;
                })
                .collect(Collectors.toList());
    }

    /**
     * 3. RSI 값이 30 이상인 코인 필터링
     */
    public List<String> getRSIFilteredCoins(List<String> coins) {
        return coins.stream()
                .filter(coin -> calculateRSI(coin, 14) > 30)
                .collect(Collectors.toList());
    }

    /**
     * 4. 매수 주문량이 매도 주문량보다 큰 코인 필터링
     */
    public String getOrderBookFilteredCoins(List<String> coins) {
        return coins.stream()
                .filter(this::isBuyOrderMoreThanSell)
                .collect(Collectors.toList())
                .get(0);
    }

    /**
     * ✅ 최종 매수 대상 탐색
     */
    public String findCoinsToTrade() {
        List<String> topCoins = getTopTradedCoins();
        List<String> highVolumeCoins = getHighVolumeCoins(topCoins);
        List<String> rsiFilteredCoins = getRSIFilteredCoins(highVolumeCoins);
        return getOrderBookFilteredCoins(rsiFilteredCoins);
    }

    /**
     * 📌 캔들 데이터 가져오기
     */
    private List<Map<String, Object>> getCandleData(String market, String unit, int count) {
        String url = String.format("/candles/minutes/%s?market=%s&count=%d", unit, market, count);
        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(List.class)
                .block();
    }

    /**
     * 📌 RSI 계산 로직
     */
    private double calculateRSI(String market, int period) {
        List<Map<String, Object>> candles = getCandleData(market, "1", period * 2);
        List<Double> closes = candles.stream().map(c -> (Double) c.get("trade_price")).collect(Collectors.toList());

        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i < period + 1; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) avgGain += change;
            else avgLoss -= change;
        }
        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * 📌 매수 주문량 > 매도 주문량 체크
     */
    private boolean isBuyOrderMoreThanSell(String market) {
        String url = "/orderbook?markets=" + market;
        List<Map<String, Object>> response = webClient.get().uri(url)
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (response == null || response.isEmpty()) return false;

        List<Map<String, Object>> orderbookUnits = (List<Map<String, Object>>) response.get(0).get("orderbook_units");
        double totalBidSize = orderbookUnits.stream().mapToDouble(o -> (Double) o.get("bid_size")).sum();
        double totalAskSize = orderbookUnits.stream().mapToDouble(o -> (Double) o.get("ask_size")).sum();

        return totalBidSize > totalAskSize;
    }

    /**
     * 📌 전체 코인 리스트 가져오기
     */
    private String getAllMarkets() {
        List<Map<String, Object>> response = webClient
                                                .get()
                                                .uri("/market/all")
                                                .retrieve()
                                                .bodyToMono(List.class)
                                                .block();

        return response.stream()
                .map(data -> (String) data.get("market"))
                .collect(Collectors.joining(","));
    }
}
