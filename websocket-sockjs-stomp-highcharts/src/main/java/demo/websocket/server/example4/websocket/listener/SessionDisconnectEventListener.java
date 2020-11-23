package demo.websocket.server.example4.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class SessionDisconnectEventListener implements ApplicationListener<SessionDisconnectEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SessionDisconnectEventListener.class);

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        logger.info("Session disconnected: {}", event);
    }
}
