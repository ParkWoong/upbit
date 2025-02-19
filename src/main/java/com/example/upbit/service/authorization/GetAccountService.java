package com.example.upbit.service.authorization;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.upbit.properties.AuthEndPointProperties;

import static com.example.upbit.config.WebClientConfig.getSend;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAccountService {
    
    //private final WebClient webClient;
    private final AuthEndPointProperties endPointProperties;
    private final static ParameterizedTypeReference<List<Map<String, Object>>> MAP_TYPE =
                                         new ParameterizedTypeReference<List<Map<String,Object>>>() {};

    

    // @SuppressWarnings("null")
    public Map<String, Object> getAccount(){
        
        final String endPoint = endPointProperties.getGetAccount();

        final ResponseEntity<List<Map<String, Object>>> response = 
                                    getSend(endPoint, null, null, MAP_TYPE);

        List<Map<String, Object>> responseBody = response.getBody();
        
        if(responseBody != null){
            return responseBody.get(0);
        }else return null;
    }
}
