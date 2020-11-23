package demo.websocket.client.example1;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ClientWebSocketApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientWebSocketApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}

