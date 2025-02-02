package com.example.upbit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.upbit.properties.TradeKeyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import static com.example.upbit.config.WebClientConfig.postSend;

@Service
@RequiredArgsConstructor
public class CoinTradeService {

    private volatile static BigDecimal AMOUNT = new BigDecimal("100000");
    private volatile boolean TRADING_ACTIVE = true; // 트레이딩 활성 상태
    private volatile static BigDecimal CURRENT_VOLMUE;

    private final String TRADE_ENDPOINT = "https://api.upbit.com/v1/order";
    private final GetCoinService getCoinService;
    private final TradeKeyProperties tradeKeyProperties;
    private final ObjectMapper objectMapper;

    public boolean tryTrade(final String startPrice) {

        while (TRADING_ACTIVE) {
            try {
                if(startPrice != null) AMOUNT = new BigDecimal(startPrice);

                // 매매법에 따른 코인 가져오기
                final String coin = getCoinService.findBuyTargets().get(0);

                // 코인의 최초 현재 시장가
                final BigDecimal marketPrice = new BigDecimal(getCoinService
                        .getTickerData(coin)
                        .get(tradeKeyProperties.getTradePrice()).toString());

                // + 0.03%
                final BigDecimal profitPrice = marketPrice.multiply(BigDecimal.valueOf(1.003));
                // - 0.1%
                final BigDecimal lossPrice = marketPrice.multiply(BigDecimal.valueOf(0.99));
                // -0.13%
                //final BigDecimal stopLossPrice = marketPrice.multiply(BigDecimal.valueOf(0.987));

                placeMarketBuyOrder(TRADE_ENDPOINT, coin, AMOUNT);

                while (TRADING_ACTIVE) {

                    TimeUnit.SECONDS.sleep(1);
                    // 1초마다 확인

                    BigDecimal currentPrice = new BigDecimal(getCoinService
                            .getTickerData(coin)
                            .get(tradeKeyProperties.getTradePrice()).toString());
                    
                    if (currentPrice.compareTo(profitPrice) >= 0) {
                        placeMarketBuyOrder(TRADE_ENDPOINT, coin, AMOUNT);
                        break;
                    } else if (currentPrice.compareTo(lossPrice) <= 0) {
                        placeMarketSellOrder(TRADE_ENDPOINT, coin);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 최초 거래 가격 설정정

        }

        return false;

    }

    // ========================================================
    // 1. 시장가 매수 (KRW 기준 금액만큼 매수)
    // ========================================================
    @SuppressWarnings("unchecked")
    public Map<String, Object> placeMarketBuyOrder(String endPoint, String market, BigDecimal amount) {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("market", market);
        requestBody.put("side", "bid"); // 매수
        requestBody.put("price", amount.toPlainString());
        requestBody.put("ord_type", "price");

        final String responseBody = postSend(endPoint, null, requestBody, null).getBody();

        Map<String, Object> resultMap = new HashMap<>();

        try {
            resultMap = objectMapper.readValue(responseBody, Map.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        CURRENT_VOLMUE = new BigDecimal(resultMap.get("executed_volume").toString());

        return resultMap;
    }

    // =====================================================================
    // 2. 지정가 매도 (보유한 모든 코인을 특정 가격에 매도)
    // =====================================================================
    public void placeLimitSellOrder(String endPoint, String market, BigDecimal price) {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("market", market);
        requestBody.put("side", "ask"); // 매도
        requestBody.put("volume", CURRENT_VOLMUE.toPlainString()); // 보유한 코인 개수
        requestBody.put("price", price.toPlainString()); // 지정된 가격
        requestBody.put("ord_type", "limit"); // 지정가 매도

        postSend(endPoint, null, requestBody, null);
    }

    // =====================================================================
    // 3. 시장가 매도(즉시 시장가에 가지고 있는 모든 코인을 매도)
    // =====================================================================
    public void placeMarketSellOrder(String endPoint, String market) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("market", market);
        requestBody.put("side", "ask"); // 매도
        requestBody.put("volume", CURRENT_VOLMUE.toPlainString()); // 보유한 코인 개수
        requestBody.put("ord_type", "market"); // 시장가 매도

        postSend(endPoint, null, requestBody, null);
    }

    //===================
    // TEST
    //===================
    public void testBuy(final BigDecimal marketPrice){
        CURRENT_VOLMUE = AMOUNT.divide(marketPrice, 8, RoundingMode.DOWN);
    }

}
