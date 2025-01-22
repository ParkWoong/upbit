package com.example.upbit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MarketBidDTO {
    
    private String market;
    
    // bid : 매수
    // ask : 매도
    private String side;

    private String price;
    

    // price : 시장가 매수
    // market : 시장가 매도
    // limit : 지정가 주문
    private String ord_type;
}
