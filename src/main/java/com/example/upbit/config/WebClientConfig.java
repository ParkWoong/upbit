package com.example.upbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;


import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebClientConfig {

    private final ExchangeFilterFunction exchaneFilter;

    private static WebClient webClient;
    private static final String ENCODINGHEADER = "accept-encoding";
    private static final String ENCODINGVALUE = "identity";
    private static byte[] EMPTY_BODY = {};

    private HttpClient defaultHttpClient() {
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

    private WebClient webClient(HttpClient httpClient){
        return WebClient
        .builder()
        // Use ExchangeFilterFunction
        .filter(exchaneFilter)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build()
        ;
    }


    @Bean
    public WebClient defaultWebClient(){
        return webClient = webClient(defaultHttpClient());
    }

    public static <T> ResponseEntity<String> postSend(
                        final String endPoint,
                        final HttpHeaders headers,
                        final T requestBody,
                        final MultiValueMap<String, String> params) {
                                
        log.info("  SERVICE) Call Post API endPoint : {}", endPoint);

        ResponseEntity<String> responseFromExternal = webClient
                        .post()
                        .uri(endPoint, uriBuilder ->
                                    uriBuilder.queryParams(params).build())
                        .contentType(MediaType.APPLICATION_JSON) // default Content Type
                        .headers(h -> {
                                if(headers != null)
                                    h.addAll(headers);
                                h.set(ENCODINGHEADER, ENCODINGVALUE);
                        })
                        //body(new CachingBodyInserter<>(BodyInserters.fromValue(requestBody)))
                        .bodyValue(requestBody!=null?requestBody:EMPTY_BODY)
                        .retrieve()
                        .toEntity(String.class)
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
                            if(headers != null)
                                h.addAll(headers);
                            h.set(ENCODINGHEADER, ENCODINGVALUE);
                        })
                        .retrieve()
                        .toEntity(responseType)
                        .block();
    }

    // ExchangeFilterFunction cachingBodyInserterFilter = ExchangeFilterFunction.ofRequestProcessor(request -> {
    //     if (request.method() == HttpMethod.POST) {
    //         // 타입 캐스팅
    //         BodyInserter<?, ReactiveHttpOutputMessage> castedInserter =
    //                 (BodyInserter<?, ReactiveHttpOutputMessage>) request.body();
    
    //         if (!(castedInserter instanceof CachingBodyInserter)) {
    //             CachingBodyInserter<ReactiveHttpOutputMessage> cachingInserter =
    //                     new CachingBodyInserter<>(castedInserter);
    
    //             ClientRequest newRequest = ClientRequest.from(request)
    //                     .body(cachingInserter)
    //                     .build();
    
    //             return Mono.just(newRequest);
    //         }
    //     }
    //     return Mono.just(request);
    // });

}
