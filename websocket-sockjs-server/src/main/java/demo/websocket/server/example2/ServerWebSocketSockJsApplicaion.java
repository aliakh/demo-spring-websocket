package demo.websocket.server.example2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerWebSocketSockJsApplicaion {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebSocketSockJsApplicaion.class, args);
    }
}
