package demo.websocket.client.example3;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ClientWebSocketSockJsStompApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientWebSocketSockJsStompApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
