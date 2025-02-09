package com.example.upbit.history.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "upbit_trade_his_log")
public class TradeHis {
    
    
    @Id
    private String uuid;

    @Builder.Default
    private LocalDateTime tradeTime = LocalDateTime.now();
    
    private String coin;    // 코인명명
    private String startAmount; // 거래 시작 금액
    private String tradedAmount;   // 거래 된 금액
    private String type;    // 손/익 타임
    private String diffAmount;  // 거래 차액
}
