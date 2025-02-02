package com.example.upbit.config;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpOutputMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SuppressWarnings({ "method", "null" })
public class CachingHttpOutputMessage implements ReactiveHttpOutputMessage {

    private final ReactiveHttpOutputMessage delegate;
    private Mono<String> cachedBodyMono;

    public CachingHttpOutputMessage(ReactiveHttpOutputMessage delegate) {
        this.delegate = delegate;
    }

    /**
     * 캐싱된 본문 데이터를 Mono<String>으로 반환.
     */
    public Mono<String> getCachedBody() {
        return cachedBodyMono;
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // body로 전달된 DataBuffer들을 모두 join하여 하나의 DataBuffer로 만든 후, 내용을 문자열로 캐싱
        Mono<DataBuffer> joined = DataBufferUtils.join(Flux.from(body));
        cachedBodyMono = joined.map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return new String(bytes, StandardCharsets.UTF_8);
        });

        // 캐싱한 후, delegate에 동일한 데이터를 writeWith()를 통해 전달한다.
        return joined.flatMap(dataBuffer -> {
            DataBufferFactory bufferFactory = delegate.bufferFactory();
            // 읽은 데이터를 다시 wrap하여 delegate에 쓴다.
            DataBuffer newBuffer = bufferFactory.wrap(getBytes(dataBuffer));
            return delegate.writeWith(Mono.just(newBuffer));
        });
    }

    /**
     * DataBuffer의 내용을 byte[]로 복사하는 헬퍼 메서드.
     */
    private byte[] getBytes(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return bytes;
    }

    // 아래 메서드들은 delegate에 위임하거나, 필요한 경우 직접 구현한다.
    
    @Override
    public HttpHeaders getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public DataBufferFactory bufferFactory() {
        return delegate.bufferFactory();
    }

    @Override
    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
        delegate.beforeCommit(action);
    }

    @Override
    public boolean isCommitted() {
        return delegate.isCommitted();
    }

    @Override
    public Mono<Void> setComplete() {
        return delegate.setComplete();
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return delegate.writeAndFlushWith(body);
    }
}