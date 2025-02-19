package com.example.upbit.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties("upbit.endpoint.auth")
public class AuthEndPointProperties {
    private String getAccount;
    private String order;
}
