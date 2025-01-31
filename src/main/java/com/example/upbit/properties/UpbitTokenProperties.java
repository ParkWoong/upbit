package com.example.upbit.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("upbit")
@Getter
@Setter
public class UpbitTokenProperties {
    private String accessKey;
    private String secretKey;
}
