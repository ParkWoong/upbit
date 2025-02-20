package com.example.upbit.service.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class TradeStopEvent extends ApplicationEvent{

    public TradeStopEvent(Object source) {
        super(source);
    }
    
}
