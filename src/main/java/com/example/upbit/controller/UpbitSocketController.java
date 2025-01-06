package com.example.upbit.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.upbit.service.socket.UpbitSocketService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
public class UpbitSocketController {
    
    private final UpbitSocketService upbitService;
    
    @GetMapping("/socket/get/current/tradingInfo")
    public void connectSocket() {

        System.out.println("Local Spring was called");

        upbitService.connectToWebSocket();
    }
    
    @GetMapping("/socket/get/data")
    public String getMethodName() {
        return upbitService.getTopTwoMarkets().toString();
    }
    

    @GetMapping("/socket/stop")
    public void disconnectSocket() {
        upbitService.disconnect();
    }
    
}
