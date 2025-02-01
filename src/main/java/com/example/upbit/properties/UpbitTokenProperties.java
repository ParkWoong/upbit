package com.example.upbit.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("upbit")
@Getter
@Setter
@Configuration
@Component
public class UpbitTokenProperties {
    private String accessKey;
    private String secretKey;
}
