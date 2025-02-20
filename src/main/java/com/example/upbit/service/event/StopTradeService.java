package com.example.upbit.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StopTradeService {
    private final TradingStatus tradingStatus;
    private final ApplicationEventPublisher eventPublisher;

    public boolean stopTrade() {
        log.info("Publishing TradeStopEvent...");
        eventPublisher.publishEvent(new TradeStopEvent(this));  // 이벤트 발생
        return tradingStatus.isTRADING_ACTIVE();
    }
}
