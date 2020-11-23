package demo.websocket.server.example4.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class SessionSubscribeEventListener implements ApplicationListener<SessionSubscribeEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SessionSubscribeEventListener.class);

    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        logger.info("Session subscribed: {}", event);
    }
}
