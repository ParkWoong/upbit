package com.example.upbit.config;

import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.upbit.properties.UpbitTokenProperties;
import com.example.upbit.util.JWTUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Configuration
public class TokenFilterFunction implements ExchangeFilterFunction {
    
    
    private final UpbitTokenProperties upbitTokenProperties;
    private final static String tokenRequestEndPoint = "test";
    
    @Override
    @SuppressWarnings({ "method", "null", "unchecked" })
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        if(request.url().toString().equals(tokenRequestEndPoint))
            // 1. GET 요청: 쿼리 파라미터 추출
            if (request.method() == HttpMethod.GET) {

                String jwt = JWTUtil.makeToken(upbitTokenProperties.getAccessKey(),
                                                upbitTokenProperties.getSecretKey(),
                                                getQueryFromRequest(request));
                
                return next.exchange(withAuthHeader(request, jwt));
            }

            // 2. POST 요청: Body 추출 (Reactive Streams 보존)
            if (request.method() == HttpMethod.POST) {
                return DataBufferUtils.join((Publisher<? extends DataBuffer>) request.body())
                        .flatMap(dataBuffer -> {
                            // Body를 String으로 변환 (소멸 없이)
                            String bodyContent = dataBuffer.toString(StandardCharsets.UTF_8);
                            
                            DataBufferUtils.release(dataBuffer); // 버퍼 해제

                            // JWT 생성 및 헤더 추가
                            String jwt = JWTUtil.makeToken(upbitTokenProperties.getAccessKey(),
                                                            upbitTokenProperties.getSecretKey(),
                                                            bodyContent);
                            
                        
                            ClientRequest newRequest = withAuthHeader(request, jwt);

                            // Body를 다시 DataBuffer로 변환하여 재사용
                            DataBuffer newBody = dataBuffer.factory().wrap(bodyContent.getBytes());
                            
                            ClientRequest finalRequest = ClientRequest.from(newRequest)
                                    .body((BodyInserter<?, ? super ClientHttpRequest>) Mono.just(newBody))
                                    .build();

                            return next.exchange(finalRequest);
                        });
            }else{
                return next.exchange(request);
            }
            
    }

        private ClientRequest withAuthHeader(ClientRequest request, String jwt) {
        return ClientRequest.from(request)
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .build();
    }

    // GET 요청의 쿼리 파라미터 추출
    private String getQueryFromRequest(ClientRequest request) {
        return UriComponentsBuilder.fromUri(request.url())
                .build()
                .getQuery();
    }
    
}
