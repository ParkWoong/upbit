package com.example.upbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {

    private static WebClient webClient;
    private static final String ENCODINGHEADER = "accept-encoding";
    private static final String ENCODINGVALUE = "identity";
    private static byte[] EMPTY_BODY = {};
    private static String BASE_URL = "https://api.upbit.com";

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
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build()
        ;
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
                        .uri(BASE_URL, uriBuilder ->
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

    public static <R> ResponseEntity<?> getSend(final String path,
                                                final HttpHeaders headers,
                                                final MultiValueMap<String, String> params,
                                                final ParameterizedTypeReference<R> responseType){
        return webClient.get()
                        .uri(BASE_URL, uriBuilder -> uriBuilder.queryParams(params).build())
                        .headers(h -> {
                            h.addAll(headers);
                            h.set(ENCODINGHEADER, ENCODINGVALUE);
                        })
                        .retrieve()
                        .toEntity(responseType)
                        .block();
    }

}
