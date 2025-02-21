package com.example.upbit.service.authorization;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.upbit.service.CoinTradeService;

import static com.example.upbit.config.WebClientConfig.postSend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final GetInfoService getInfoService;

    private final static ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = 
                                                new ParameterizedTypeReference<Map<String,Object>>() {};

    // ========================================================
    // 1. 시장가 매수 (KRW 기준 금액만큼 매수)
    // ========================================================
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



        Map<String, Object>responseBody =  new HashMap<>();

        try {
            responseBody = postSend(endPoint, null, requestBody, param, MAP_TYPE).getBody();    
        } catch(WebClientResponseException e){
            responseBody = e.getResponseBodyAs(MAP_TYPE);
            return responseBody;
        } 

        String tradeStatus = null;
        String tradeVolume = null;

        Map<String, Object> coinTradeStatue = null;

        if(responseBody != null){
                        
            final String uuid = responseBody.get("uuid").toString();

            do {
                coinTradeStatue = getInfoService
                                        .getCoinTradeInfo(market, uuid);

                if(coinTradeStatue.containsKey("status")){
                    tradeStatus = String.valueOf(coinTradeStatue.get("status"));
                    tradeVolume = String.valueOf(coinTradeStatue.get("executed_volume"));
                }

            } while (tradeStatus == null || !tradeStatus.equals("done"));            
        }
        
        CoinTradeService.CURRENT_VOLMUE = new BigDecimal(tradeVolume);

        return coinTradeStatue;
    }

    // =====================================================================
    // 2. 시장가 매도(즉시 시장가에 가지고 있는 모든 코인을 매도)
    // =====================================================================
    public Map<String, Object> placeMarketSellOrder(String endPoint, String market) {

        MultiValueMap<String, String> requestParam = new LinkedMultiValueMap<>();
        requestParam.add("market", market);
        requestParam.add("side", "ask"); // 매도
        requestParam.add("volume", "1"); // 보유한 코인 개수
        //requestParam.add("volume", CoinTradeService.CURRENT_VOLMUE.toPlainString()); // 보유한 코인 개수
        requestParam.add("ord_type", "market"); // 시장가 매도

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("market", market);
        requestBody.put("side", "ask"); // 매도
        requestBody.put("volume", "1"); // 보유한 코인 개수
        //requestBody.put("volume", CoinTradeService.CURRENT_VOLMUE.toPlainString()); // 보유한 코인 개수
        requestBody.put("ord_type", "market"); // 시장가 매도

        Map<String, Object> responseBody = null;

        try {
            responseBody = postSend(endPoint, null, requestBody, requestParam, MAP_TYPE).getBody();
        } catch (WebClientResponseException e) {
            responseBody = e.getResponseBodyAs(MAP_TYPE);
            return responseBody;
        }

        String tradeStatus = null;

        Map<String, Object> coinTradeStatue = null;

        if(responseBody != null){
                        
            final String uuid = responseBody.get("uuid").toString();

            do {
                coinTradeStatue = getInfoService
                                        .getCoinTradeInfo(market, uuid);

                if(coinTradeStatue.containsKey("status")){
                    tradeStatus = coinTradeStatue.get("status").toString();                
                }
            } while (tradeStatus == null || !tradeStatus.equals("done"));            
        }

        return coinTradeStatue;

    }
}
