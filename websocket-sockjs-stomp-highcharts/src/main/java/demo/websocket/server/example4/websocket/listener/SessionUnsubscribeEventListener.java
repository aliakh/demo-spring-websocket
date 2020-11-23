package demo.websocket.server.example4.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class SessionUnsubscribeEventListener implements ApplicationListener<SessionUnsubscribeEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SessionUnsubscribeEventListener.class);

    @Override
    public void onApplicationEvent(SessionUnsubscribeEvent event) {
        logger.info("Session unsubscribes: {}", event);
    }
}
