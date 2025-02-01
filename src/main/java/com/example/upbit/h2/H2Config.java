package com.example.upbit.h2;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class H2Config {
    private final static String H2_PORT = "9090";
    private Server webServer;

    @EventListener
    public void start(ContextRefreshedEvent event) throws SQLException{
        webServer = Server
                        .createWebServer("-webPort", H2_PORT, "-tcpAllowOthers")
                        .start();
    }

    @EventListener
    public void stop(ContextClosedEvent event){
        webServer.stop();
    }
}
