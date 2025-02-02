package com.example.upbit.config;

import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;

import reactor.core.publisher.Mono;

@SuppressWarnings({ "method", "null" })
public class CachingBodyInserter<T> implements BodyInserter<T, ReactiveHttpOutputMessage> {

    private final BodyInserter<T, ReactiveHttpOutputMessage> delegate;
    // 캐싱된 body를 저장하는 Mono (여기서는 String 타입으로 가정)
    private Mono<String> cachedBodyMono;

    public CachingBodyInserter(BodyInserter<T, ReactiveHttpOutputMessage> delegate) {
        this.delegate = delegate;
    }

    /**
     * 이미 캐싱된 요청 본문을 Mono<String>으로 반환.
     */
    public Mono<String> getCachedBody() {
        if (cachedBodyMono == null) {
            return Mono.error(new IllegalStateException("Body has not been cached yet."));
        }
        return cachedBodyMono;
    }

    @Override
    public Mono<Void> insert(ReactiveHttpOutputMessage outputMessage, Context context) {
        // outputMessage를 캐싱 가능한 데코레이터로 감싼다.
        CachingHttpOutputMessage cachingOutputMessage = new CachingHttpOutputMessage(outputMessage);
        // delegate를 통해 실제 body write를 수행한다.
        return delegate.insert(cachingOutputMessage, context)
            // writeWith()가 완료된 후, 캐싱된 body를 얻어 cachedBodyMono에 할당
            .then(cachingOutputMessage.getCachedBody().doOnNext(cachedBody -> {
                this.cachedBodyMono = Mono.just(cachedBody);
            }))
            .then(); // 최종적으로 Mono<Void> 반환
    }

    
}


