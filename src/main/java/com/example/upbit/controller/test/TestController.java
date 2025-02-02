package com.example.upbit.controller.test;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.example.upbit.config.WebClientConfig.postSend;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    
    @GetMapping("/test")
    public String test() {
        //return getCoinService.getAllMarkets().toString();

        final String endPoint = "http://localhost:8080/requestBodyTest";

        Map<String, String> testMap = new HashMap<>();

        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        
        return  postSend(endPoint, null, testMap, null).getBody();
        
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
    
}
