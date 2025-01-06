package com.example.upbit.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.upbit.config.WebClientConfig.getSend;
//import static com.example.upbit.config.WebClientConfig.postSend;

import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UpbitSocketService {
    private final WebClient webClient;
    
    private Disposable connection;
    private final Set<String> activeTrades = new HashSet<>();
    private static final String SOCKET_URL = "wss://api.upbit.com/websocket/v1";
    private final Map<String, Double> tradingVolumes = new ConcurrentHashMap<>();

    private boolean isConnected = false;

    public void connectToWebSocket() {
        final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        System.out.println("Websocket Connect");

        connection = client.execute(
                URI.create(SOCKET_URL),
                session -> {
                    isConnected = true; // 연결 성공 시 상태 변수 갱신
                    session.send(Mono.just(session.textMessage("[{\"ticket\":\"unique-id\"}," +
                            "{\"type\":\"ticker\",\"codes\":[],\"isOnlyRealtime\":true}]")))
                            .then()
                            .doOnTerminate(() -> {
                                isConnected = false; // 연결 종료 시 상태 변수 갱신
                                System.out.println("WebSocket connection terminated");
                            })
                            .subscribe();

                    Mono<Void> output = session.send(Mono.just(session.textMessage("message")));
                    Mono<Void> input = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(this::processMessage)
                            .doOnError(e -> System.err.println("Error is occured " + e.getMessage()))
                            .then();

                    return Mono.zip(output, input).then();
                }
        ).doOnTerminate(() -> System.out.println("WebSocket connection terminated"))
        .doOnSuccess(aVoid -> System.out.println("WebSocket connection established"))
        .doOnError(throwable -> System.err.println("Error while establishing WebSocket connection: " + throwable.getMessage()))
        .subscribe();
    }

    private void processMessage(String message) {
        try {
            // JSON 데이터 파싱
            Map<String, Object> data = new ObjectMapper().readValue(message, Map.class);

            System.out.println("===From WebSocek Data===");
            System.out.println(data.toString());
            System.out.println("========================");

            String market = (String) data.get("code"); // 코인 마켓 코드
            Double tradeVolume = (Double) data.get("acc_trade_volume_24h"); // 24시간 거래량

            // 거래량 데이터 저장
            tradingVolumes.put(market, tradeVolume);

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    public List<String> getTopTwoMarkets() {
        return tradingVolumes.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 거래량 내림차순 정렬
                .limit(2) // 상위 2개 추출
                .map(Map.Entry::getKey)
                .toList();
    }

    public void disconnect(){
        if(connection != null || !connection.isDisposed()){
            connection.dispose();
            System.out.println("WebSocket connection is closed");
        }else{
            System.out.println("WebSocket connection was already closed");
        }
    }

    
}
