package com.example.upbit.service.authorization;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.upbit.service.CoinTradeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.upbit.config.WebClientConfig.postSend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final ObjectMapper objectMapper;

    // ========================================================
    // 1. 시장가 매수 (KRW 기준 금액만큼 매수)
    // ========================================================
    @SuppressWarnings("unchecked")
    public Map<String, Object> placeMarketBuyOrder(String endPoint, String market, String amount) {

        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("market", market);
        param.add("side", "bid"); // 매수
        param.add("price", amount);
        param.add("ord_type", "price");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("market", market);
        requestBody.put("side", "bid"); // 매수
        requestBody.put("price", amount);
        requestBody.put("ord_type", "price");



        String responseBody =  null ;

        try {
            responseBody = postSend(endPoint, null, requestBody, param).getBody();    
        } catch(WebClientResponseException e){
            Map<String, String> errorResponse = e.getResponseBodyAs(Map.class);

            log.info("{}", errorResponse);
        } 
        catch (HttpClientErrorException | HttpServerErrorException e) {
            log.info("{}", e.getStatusCode());

            Map<String, String> errorResponse = e.getResponseBodyAs(Map.class);

            log.info("{}", errorResponse);
        }
        

        Map<String, Object> resultMap = new HashMap<>();

        try {
            resultMap = objectMapper.readValue(responseBody, Map.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        CoinTradeService.CURRENT_VOLMUE = new BigDecimal(resultMap.get("executed_volume").toString());

        return resultMap;
    }
}
