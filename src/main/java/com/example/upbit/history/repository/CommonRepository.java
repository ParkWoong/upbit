package com.example.upbit.history.repository;

import org.springframework.stereotype.Repository;

import com.example.upbit.history.entity.TradeHis;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommonRepository {
    
    private final TradeHisRepository tradeHisRepository;
    
    public void saveTrade(TradeHis trade){
        tradeHisRepository.save(trade);
    }
}
