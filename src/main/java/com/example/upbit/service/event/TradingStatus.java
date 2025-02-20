package com.example.upbit.service.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class TradingStatus {
    protected volatile boolean IS_TRADING = false;
    protected volatile boolean TRADING_ACTIVE = true;

    public synchronized void setTrading(boolean trading) {
        this.IS_TRADING = trading;
        if (!trading) {
            setTradingActive(false);
        }
    }

    public synchronized void setTradingActive(boolean active) {
        this.TRADING_ACTIVE = active;
    }

    @EventListener
    public void handleTradeStopEvent(TradeStopEvent event) {
        setTradingActive(false);
    }
}
