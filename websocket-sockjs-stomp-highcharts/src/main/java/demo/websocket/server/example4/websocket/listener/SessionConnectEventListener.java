package demo.websocket.server.example4.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

@Component
public class SessionConnectEventListener implements ApplicationListener<SessionConnectEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SessionConnectEventListener.class);

    @Override
    public void onApplicationEvent(SessionConnectEvent event) {
        logger.info("Session connects: {}", event);
    }
}
