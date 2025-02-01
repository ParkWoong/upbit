package com.example.upbit.history.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.upbit.history.entity.TradeHis;

@Repository
public interface TradeHisRepository extends JpaRepository<TradeHis, String>{} 
