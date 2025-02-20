package com.example.upbit.properties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Set<String> getAuthEndPoint(){
        return Arrays
                .asList(this.getAccount, this.order)
                .stream()
                .collect(Collectors.toSet());
    }
}
