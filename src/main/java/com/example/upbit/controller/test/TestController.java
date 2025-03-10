package com.example.upbit.controller.test;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.properties.AuthEndPointProperties;
import com.example.upbit.service.ShortTermTrendCoinService;
import com.example.upbit.service.authorization.GetInfoService;
import com.example.upbit.service.authorization.TradeService;
import com.example.upbit.text.TelegramTextService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final ShortTermTrendCoinService shortTermTrendCoinService;
    private final TelegramTextService telegramTextService;
    private final GetInfoService getAccountService;
    private final TradeService tradeService;
    private final AuthEndPointProperties authEndPointProperties;
    
    @GetMapping("/test")
    public void test() {
        log.info("TEST");
        log.info("Coin : {}", shortTermTrendCoinService.findCoinsToTrade());
    }

    @PostMapping("/requestBodyTest")
    public String postTest(@RequestHeader HttpHeaders requestHeader,  @RequestBody String requestBody){
        
        log.info("Header : {}", requestHeader.toString());
        log.info("Body : {}", requestBody);
        
        return "TRUE";
    }

    @GetMapping("/text/test")
    public boolean getMethodName() {
        return telegramTextService.sendText("Hello World!");
    }

    @GetMapping("/authorization/test")
    public String authTest() {
        return getAccountService.getAccount().toString();
    }
    
    @GetMapping("/test/bid")
    public Map<String, Object> testBidTrade() {
        return tradeService.placeMarketBuyOrder(authEndPointProperties.getOrder(), "KRW-JTO", "100000");
    }

    @GetMapping("/test/ask")
    public Map<String, Object> testAskTrade() {
        return tradeService.placeMarketSellOrder(authEndPointProperties.getOrder(), "KRW-JTO"); 
    }
    @PostMapping("/local/return")
    public byte[] returnData(@RequestBody byte[] entity) {
        return entity;
    }
    
    
}
