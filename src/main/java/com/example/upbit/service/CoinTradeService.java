package com.example.upbit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.upbit.history.entity.TradeHis;
import com.example.upbit.history.repository.CommonRepository;
import com.example.upbit.history.repository.TradeHisRepository;
import com.example.upbit.properties.TradeKeyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import static com.example.upbit.config.WebClientConfig.postSend;
import static com.example.upbit.util.UUIDUtils.createUUID;

@Service
@RequiredArgsConstructor
public class CoinTradeService {

    //=======================================
    // marketPrice → API로 받아온 시장가
    // BALANCE → 현재 내 잔액 (10만 원)
    // START_BALANCE → 매수에 사용한 금액
    // TRADED_BALANCE → 매도 한 금액
    // BALANCE_AFTER_BUY -> 매수 후 남은 금액
    // BALANCE_AFTER_SELL -> 매도 후 남은 금액
    // CURRENT_AMOUNT → 매수 후 보유 코인 수량
    //=======================================

    protected volatile static BigDecimal BALANCE = new BigDecimal("100000");
    protected volatile static BigDecimal START_BALANCE = new BigDecimal("100000");
    protected volatile static BigDecimal TRADED_BALANCE;
    protected volatile static BigDecimal BALANCE_AFTER_BUY;
    protected volatile static BigDecimal BALANCE_AFTER_SELL;
    
    protected volatile boolean TRADING_ACTIVE = true; // 트레이딩 활성 상태
    protected volatile static BigDecimal CURRENT_VOLMUE;

    private final String TRADE_ENDPOINT = "https://api.upbit.com/v1/order";
    private final GetCoinService getCoinService;
    private final TradeKeyProperties tradeKeyProperties;
    private final ObjectMapper objectMapper;

    private final CommonRepository commonRepository;

    public boolean tryTrade(final String startPrice) {

        while (TRADING_ACTIVE) {

            final String tradeId = createUUID();

            try {
                if(startPrice != null) START_BALANCE = new BigDecimal(startPrice);

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

                placeMarketBuyOrder(TRADE_ENDPOINT, coin, START_BALANCE);

                while (TRADING_ACTIVE) {

                    TimeUnit.SECONDS.sleep(1);
                    // 1초마다 확인

                    BigDecimal currentPrice = new BigDecimal(getCoinService
                            .getTickerData(coin)
                            .get(tradeKeyProperties.getTradePrice()).toString());
                    
                    if (currentPrice.compareTo(profitPrice) >= 0) {
                        placeMarketBuyOrder(TRADE_ENDPOINT, coin, START_BALANCE);
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
        // 현재 보유 코인 수
        CURRENT_VOLMUE = BALANCE.divide(marketPrice, 8, RoundingMode.DOWN);
        // 사용된 금액
        START_BALANCE = CURRENT_VOLMUE.multiply(marketPrice);
        // 매수 후 남은 금액
        BALANCE = BALANCE.subtract(START_BALANCE);

    }

    public void testSell(final String coin, final BigDecimal marketPrice){
        // 매도 금액
        TRADED_BALANCE = CURRENT_VOLMUE.multiply(marketPrice);
        // 매도 후 남은 금액
        BALANCE_AFTER_SELL = BALANCE_AFTER_BUY.add(TRADED_BALANCE);
        // 남은 금액 업데이트
        BALANCE = BALANCE_AFTER_SELL;
    }

    public void saveTradeHis(final String coin, final String uuid,
                            final BigDecimal starBalance, final BigDecimal tradedBalance){
        
        BigDecimal diff = starBalance.subtract(tradedBalance);
        
        TradeHis trade = TradeHis.builder()
                                .uuid(uuid)
                                .coin(coin)
                                .startAmount(starBalance.toPlainString())
                                .tradedAmount(tradedBalance.toPlainString())
                                .type((diff.compareTo(BigDecimal.ZERO) > 0) ? "PROFIT":"LOSS")
                                .diffAmount(diff.toPlainString())
                                .build();
                                
        commonRepository.saveTrade(trade);
    }


}
