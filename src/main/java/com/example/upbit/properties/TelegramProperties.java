package com.example.upbit.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties("telegram")
@Getter
@Setter
public class TelegramProperties {
    private String token;
    private String chatId;
    private String url;
}
