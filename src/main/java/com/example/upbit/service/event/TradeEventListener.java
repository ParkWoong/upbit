package com.example.upbit.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventListener {
    private final TradingStatus tradingStatus;

    @EventListener
    public void handleTradeStopEvent(TradeStopEvent event) {
        tradingStatus.setTradingActive(false);
    }
}