package demo.websocket.server.example4.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

@RestController
public class WebSocketMessageBrokerStatsController {

    @Autowired
    private WebSocketMessageBrokerStats stats;

    @RequestMapping("/stats")
    public WebSocketMessageBrokerStats getStats() {
        return stats;
    }
}
