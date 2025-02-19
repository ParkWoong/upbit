package com.example.upbit.text;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.upbit.properties.TelegramProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TelegramTextService {
    private final TelegramProperties telegramProperties;
    private final WebClient webClient;
    

    public boolean sendText(final String message){

        String payload = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\"}", telegramProperties.getChatId(), message);

        final ResponseEntity<?> response = webClient
                                                .post()
                                                .uri(telegramProperties.getUrl())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(payload)
                                                .retrieve()
                                                .toEntity(String.class)
                                                .block();

        if(response.getStatusCode().is2xxSuccessful()) return true;

        return false;
    }

    public String makeText(final String coin, final String type, final String remainBalance, final String tradedAmount){
        
        //======================================================================
        // 메세지 형식
        // [{coin} Profit / Loss] The transaction amount is {} , remain balance is {}
        //=======================================================================
        
        StringBuilder sb = new StringBuilder();

        sb.append("[")
        .append(coin)
        .append(" ")
        .append(type)
        .append("] The transaction amount is ")
        .append(tradedAmount)
        .append("and remain balance is ")
        .append(remainBalance);        
        
        return null;   
    }
}
