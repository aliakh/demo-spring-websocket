package demo.websocket.client.example2;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ClientWebSocketSockJsApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientWebSocketSockJsApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
