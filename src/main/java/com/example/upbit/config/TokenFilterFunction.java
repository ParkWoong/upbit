package com.example.upbit.config;


import java.net.URI;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.upbit.properties.AuthEndPointProperties;
import com.example.upbit.properties.UpbitTokenProperties;
import com.example.upbit.util.JWTUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class TokenFilterFunction implements ExchangeFilterFunction {

    private final UpbitTokenProperties upbitTokenProperties;
    private final AuthEndPointProperties authEndPointProperties;
    
    @Override
    @SuppressWarnings({ "method", "null" })
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        final String endPoint = "https://".concat(request.url().getHost()).concat(request.url().getPath());

        if(authEndPointProperties.getAuthEndPoint().contains(endPoint)){
            log.info("====== {} needs JWT token ======", endPoint);
            
            // 1. GET 요청: 쿼리 파라미터 추출
            if (request.method() == HttpMethod.GET) {

                String jwt = JWTUtil.makeToken(upbitTokenProperties.getAccessKey(),
                                                upbitTokenProperties.getSecretKey(),
                                                getQueryFromRequest(request));
                
                return next.exchange(withAuthHeader(request, jwt));
            }

            // 2. POST 요청: Body 추출 (Reactive Streams 보존)
            // else if (request.method() == HttpMethod.POST) {
            //     // ClientRequest의 body()를 가져옴 (BodyInserter 타입)
            //     BodyInserter<?, ? super ReactiveHttpOutputMessage> inserter = (BodyInserter<?, ? super ReactiveHttpOutputMessage>) request.body();
                
            //     // cachingInserter로 캐스팅 (애초에 클라이언트 요청 생성 시 CachingBodyInserter로 설정했다고 가정)
            //     if (inserter instanceof CachingBodyInserter) {
                    
            //         CachingBodyInserter<String> cachingInserter = (CachingBodyInserter<String>) inserter;
                    
            //         // 캐싱된 body를 Mono<String> 형태로 획득
            //         return cachingInserter
            //                     .getCachedBody()
            //                     .flatMap(bodyContent -> {

            //                         log.info("Body : {}", bodyContent);

            //                         // 요청 본문을 이용해 JWT 토큰 생성
            //                         String jwt = JWTUtil.makeToken(
            //                             upbitTokenProperties.getAccessKey(),
            //                             upbitTokenProperties.getSecretKey(),
            //                             bodyContent
            //                         );
                        
            //                         // 새로운 요청 생성: 기존 요청 정보 복사, JWT 헤더 추가, 그리고 body는 캐싱된 값을 재설정
            //                         ClientRequest newRequest = ClientRequest.from(request)
            //                             .headers(headers -> headers.setBearerAuth(jwt))
            //                             .body(BodyInserters.fromValue(bodyContent))
            //                             .build();
                                    
            //                         return next.exchange(newRequest);
            //                     });
            //     }
            // }

            else if (request.method() == HttpMethod.POST) {

                

                final String query = getQueryFromRequest(request);

                log.info("Post Request : {}", query);

                // 원본 요청 Body를 복제하기 위해 로컬 환경 복제 API 호출출
                // for ClientRequest -> ClientResponse
                return next
                        .exchange(ClientRequest
                                                .from(request)
                                                .url(URI.create("http://localhost:8080/local/return"))
                                                .build())
                        .flatMap(response -> response.bodyToMono(String.class)
                            .flatMap(bodyContent -> {
                                String jwt = JWTUtil.makeToken(
                                    upbitTokenProperties.getAccessKey(),
                                    upbitTokenProperties.getSecretKey(),
                                    query
                                );
                                
                                log.info("Request URI : {}, Request Body : {}", endPoint, bodyContent);

                                // 새 요청 생성 (기존 Body 유지)
                                ClientRequest newRequest = ClientRequest.from(request)
                                    .url(URI.create(endPoint))
                                    .headers(headers -> headers.setBearerAuth(jwt))
                                    .body(BodyInserters.fromValue(bodyContent))  // BodyInserter 활용
                                    .build();
        
                                return next.exchange(newRequest);
                            }));
            }
        }
        return next.exchange(request);
    }

    private ClientRequest withAuthHeader(ClientRequest request, String jwt) {
        return ClientRequest.from(request)
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .build();
        }

    private String getQueryFromRequest(ClientRequest request) {
        return UriComponentsBuilder.fromUri(request.url())
                .build()
                .getQuery();
    }
}
