package com.example.upbit.controller.test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.service.ShortTermTrendCoinService;
import com.example.upbit.text.TelegramTextService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final ShortTermTrendCoinService shortTermTrendCoinService;
    private final TelegramTextService telegramTextService;
    
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

    @PostMapping("/local/return")
    public byte[] returnData(@RequestBody byte[] entity) {
        return entity;
    }

    @GetMapping("/text/test")
    public boolean getMethodName() {
        return telegramTextService.sendText("Hello World!");
    }
    
    
}
