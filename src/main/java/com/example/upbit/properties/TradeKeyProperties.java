package com.example.upbit.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Configuration
@ConfigurationProperties("trade.key")
@Getter
@Setter
@ToString
public class TradeKeyProperties {
    private String fisrtCoin;
    private String secondCoin;
    private String buyPrice;
    private String sellPrice;
    private String profitMargin;
    private String lossMargin;
    private String tradePrice;
}
