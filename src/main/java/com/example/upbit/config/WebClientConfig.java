package com.example.upbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.upbit.properties.UpbitTokenProperties;
import com.example.upbit.util.JWTUtil;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebClientConfig {

    private static final UpbitTokenProperties upbitTokenProperties;

    private static WebClient webClient;
    private static final String ENCODINGHEADER = "accept-encoding";
    private static final String ENCODINGVALUE = "identity";
    private static byte[] EMPTY_BODY = {};

    @Bean
    public static HttpClient defaultHttpClient() {
        HttpClient client = HttpClient
                .create()
                .doOnConnected(con -> con
                        .addHandlerFirst(
                                new ReadTimeoutHandler(3000))
                        .addHandlerLast(
                                new WriteTimeoutHandler(3000)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
        return client;
    }

    public static WebClient webClient(HttpClient httpClient){
        return WebClient
        .builder()
        // Use ExchangeFilterFunction
        .filter(null)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build()
        ;
    }

    public ExchangeFilterFunction apply() {
        return (request, next) -> {
            // 1. GET 요청: 쿼리 파라미터 추출
            if (request.method() == HttpMethod.GET) {
                String query = UriComponentsBuilder.fromUri(request.url()).build().getQuery();
                String jwt = JWTUtil.makeToken(upbitTokenProperties.getAccessKey(), upbitTokenProperties.getSecretKey(), query);
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
                            String jwt = JWTUtil.makeToken(upbitTokenProperties.getAccessKey(), upbitTokenProperties.getSecretKey(), UriComponentsBuilder.fromUri(request.url()).build().getQuery());
                            ClientRequest newRequest = withAuthHeader(request, jwt);

                            // Body를 다시 DataBuffer로 변환하여 재사용
                            DataBuffer newBody = dataBuffer.factory().wrap(bodyContent.getBytes());
                            ClientRequest finalRequest = ClientRequest.from(newRequest)
                                    .body((BodyInserter<?, ? super ClientHttpRequest>) Mono.just(newBody))
                                    .build();

                            return next.exchange(finalRequest);
                        });
            }

            // 3. 기타 메서드
            return next.exchange(request);
        };
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



    @Bean
    public static WebClient defaultWebClient(HttpClient httpClient){
        return webClient = webClient(httpClient);
    }

    public static <T, R> ResponseEntity<?> postSend(
                        final String endPoint,
                        final HttpHeaders headers, final T requestBody,
                        final MultiValueMap<String, String> params,
                        final ParameterizedTypeReference<R> responseClass) {
                                
        log.info("  SERVICE) Call Post API endPoint : {}", endPoint);

        ResponseEntity<?> responseFromExternal = webClient
                        .post()
                        .uri(endPoint, uriBuilder ->
                                    uriBuilder.queryParams(params).build())
                        .headers(h -> {
                                h.addAll(headers);
                                h.set(ENCODINGHEADER, ENCODINGVALUE);
                        })
                        .bodyValue(requestBody != null ? requestBody : EMPTY_BODY)
                        .retrieve()
                        .toEntity(responseClass)
                        .block();


        return responseFromExternal;
    }

    public static <R> ResponseEntity<?> getSend(final String endPoint,
                                                final HttpHeaders headers,
                                                final MultiValueMap<String, String> params,
                                                final ParameterizedTypeReference<R> responseType){
        return webClient.get()
                        .uri(endPoint, uriBuilder -> uriBuilder.queryParams(params).build())
                        .headers(h -> {
                            h.addAll(headers);
                            h.set(ENCODINGHEADER, ENCODINGVALUE);
                        })
                        .retrieve()
                        .toEntity(responseType)
                        .block();
    }

}
