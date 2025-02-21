package com.example.upbit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.upbit.history.entity.TradeHis;
import com.example.upbit.history.repository.CommonRepository;
import com.example.upbit.properties.AuthEndPointProperties;
import com.example.upbit.properties.TradeKeyProperties;
import com.example.upbit.service.event.TradingStatus;
import com.example.upbit.text.TelegramTextService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.example.upbit.util.UUIDUtils.createUUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public volatile static BigDecimal CURRENT_VOLMUE = new BigDecimal(0);

    private final GetCoinService getCoinService;
    private final ShortTermTrendCoinService shortTermTrendCoinService;
    private final TradeKeyProperties tradeKeyProperties;
    private final TelegramTextService telegramTextService;
    private final TradingStatus tradingStatus;
    private final AuthEndPointProperties authEndPointProperties;

    
    private static int CURRENT_COUNT = 0;

    private final CommonRepository commonRepository;

    public boolean tryTrade(final String startPrice) {

        while (tradingStatus.isTRADING_ACTIVE()) {

            final String tradeId = createUUID();

            try {
                if(startPrice != null) BALANCE = new BigDecimal(startPrice);

                // 매매법에 따른 코인 가져오기
                String coin = null;

                do{
                    coin = shortTermTrendCoinService.findCoinsToTrade();
                    log.info("Trade Coin : {}", coin);
                }
                while (coin == null);

                //코인의 최초 현재 시장가
                final BigDecimal marketPrice = new BigDecimal(getCoinService
                        .getTickerData(coin)
                        .get(tradeKeyProperties.getTradePrice()).toString());

                // + 0.03%
                final BigDecimal profitPrice = marketPrice.multiply(BigDecimal.valueOf(1.003));
                // - 0.1%
                final BigDecimal lossPrice = marketPrice.multiply(BigDecimal.valueOf(0.99));
                // -0.13%
                //final BigDecimal stopLossPrice = marketPrice.multiply(BigDecimal.valueOf(0.987));

                //placeMarketBuyOrder(TRADE_ENDPOINT, coin, START_BALANCE);
                testBuy(marketPrice);
                
                log.info("\n 코인 : {} \n 시장가가 : {} \n 익절 가격 : {} \n 손절 가격 : {} \n 구입 금액 : {}", 
                                    coin, marketPrice, profitPrice, lossPrice, START_BALANCE);

                while (tradingStatus.isTRADING_ACTIVE()) {

                    TimeUnit.SECONDS.sleep(1);
                    // 1초마다 확인

                    BigDecimal currentPrice = new BigDecimal(getCoinService
                            .getTickerData(coin)
                            .get(tradeKeyProperties.getTradePrice()).toString());
                            
                    log.info("Current Price : {}, Call Count : {}", currentPrice, CURRENT_COUNT);
                    CURRENT_COUNT ++;
                    
                    if (currentPrice.compareTo(profitPrice) >= 0) {
                        //placeMarketBuyOrder(authEndPointProperties.getOrder(), coin, START_BALANCE);
                        testSell(coin, marketPrice);
                        saveTradeHis(coin, tradeId, START_BALANCE, TRADED_BALANCE);
                        break;
                    } else if (currentPrice.compareTo(lossPrice) <= 0) {
                        //placeMarketSellOrder(authEndPointProperties.getOrder(), coin);
                        testSell(coin, marketPrice);
                        saveTradeHis(coin, tradeId, START_BALANCE, TRADED_BALANCE);
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

    //===================
    // TEST
    //===================
    public void testBuy(final BigDecimal marketPrice){
        // 현재 보유 코인 수
        CURRENT_VOLMUE = BALANCE.divide(marketPrice, 8, RoundingMode.DOWN);
        // 사용된 금액
        START_BALANCE = CURRENT_VOLMUE.multiply(marketPrice);
        // 매수 후 남은 금액
        BALANCE_AFTER_BUY = BALANCE.subtract(START_BALANCE);
        BALANCE = BALANCE_AFTER_BUY;

        tradingStatus.setIS_TRADING(true);

    }

    public void testSell(final String coin, final BigDecimal marketPrice){
        // 매도 금액
        TRADED_BALANCE = CURRENT_VOLMUE.multiply(marketPrice);
        // 매도 후 남은 금액
        BALANCE_AFTER_SELL = BALANCE_AFTER_BUY.add(TRADED_BALANCE);
        // 남은 금액 업데이트
        BALANCE = BALANCE_AFTER_SELL;

        // stop trading;
        //TRADING_ACTIVE = false;
        
        //check either trading or not
        tradingStatus.setIS_TRADING(false);
        
        // stop counting;
        CURRENT_COUNT = 0;
    }

    public void saveTradeHis(final String coin, final String uuid,
                            final BigDecimal startBalance, final BigDecimal tradedBalance){
        
        final BigDecimal diff = tradedBalance.subtract(startBalance); 

        final String tradeResult = diff.compareTo(BigDecimal.ZERO) > 0 ? "PROFIT" : "LOSS";

        log.info("diff : {}", diff.toPlainString());
        
        final TradeHis trade = TradeHis.builder()
                                .uuid(uuid)
                                .coin(coin)
                                .startAmount(startBalance.toPlainString())
                                .tradedAmount(tradedBalance.toPlainString())
                                .type(tradeResult)
                                .diffAmount(diff.toPlainString())
                                .build();
                                
        commonRepository.saveTrade(trade);

        //==========================
        // Do Message
        // Slack or Telegram
        //==========================
        final String message = telegramTextService
                                .makeText(coin, tradeResult, BALANCE_AFTER_SELL.toPlainString(), diff.toPlainString());
        
        log.info("Message : {}", message);

        telegramTextService.sendText(message);
    }


}
