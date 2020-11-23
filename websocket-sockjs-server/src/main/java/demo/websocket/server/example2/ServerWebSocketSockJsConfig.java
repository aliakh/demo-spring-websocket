package demo.websocket.server.example2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ServerWebSocketSockJsConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(webSocketHandler(), "/websocket-sockjs")
				.setAllowedOrigins("*")
				.withSockJS()
				.setWebSocketEnabled(true)
				.setHeartbeatTime(25000)
				.setDisconnectDelay(5000)
				.setClientLibraryUrl("/webjars/sockjs-client/1.1.2/sockjs.js")
				.setSessionCookieNeeded(false);
	}

	@Bean
	public WebSocketHandler webSocketHandler() {
		return new ServerWebSocketHandler();
	}
}
