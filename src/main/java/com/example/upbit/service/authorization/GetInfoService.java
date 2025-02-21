package com.example.upbit.service.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.upbit.properties.AuthEndPointProperties;

import static com.example.upbit.config.WebClientConfig.getSend;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetInfoService {
    
    //private final WebClient webClient;
    private final AuthEndPointProperties endPointProperties;
    private final static ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP_TYPE =
                                         new ParameterizedTypeReference<List<Map<String,Object>>>() {};
    private final static ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
                                         new ParameterizedTypeReference<Map<String,Object>>() {};

    

    // @SuppressWarnings("null")
    public Map<String, Object> getAccount(){
        
        final String endPoint = endPointProperties.getGetAccount();

        final ResponseEntity<List<Map<String, Object>>> response = 
                                    getSend(endPoint, null, null, LIST_MAP_TYPE);

        List<Map<String, Object>> responseBody = response.getBody();
        
        if(responseBody != null){
            return responseBody.get(0);
        }else return null;
    }

    public Map<String, Object> getCoinTradeInfo(final String market, final String uuid){
        
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("market", market);
        query.add("uuid", Arrays.asList(uuid).toString());

        Map<String, Object> response = null;

        try {
            response = getSend(endPointProperties.getUuid(), null, query, MAP_TYPE).getBody();
        } catch (WebClientResponseException e) {
            response = e.getResponseBodyAs(MAP_TYPE);
        }

        return response;    
    }
}
