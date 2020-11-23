package demo.websocket.server.example3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerWebSocketSockJsStompApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebSocketSockJsStompApplication.class, args);
    }
}
