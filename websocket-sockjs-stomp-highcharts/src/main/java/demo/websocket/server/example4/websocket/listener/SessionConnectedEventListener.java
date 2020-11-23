package demo.websocket.server.example4.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Component
public class SessionConnectedEventListener implements ApplicationListener<SessionConnectedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SessionConnectedEventListener.class);

    @Override
    public void onApplicationEvent(SessionConnectedEvent event) {
        logger.info("Session connected: {}", event);
    }
}
