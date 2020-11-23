package demo.websocket.server.example4.websocket.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

public class LoggingChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        logger.info("Before the message {} is send to {}", message, channel);
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        logger.info("After the message {} is send to {}", message, channel);
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        logger.info("Before a message is received from {}", channel);
        return true;
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        logger.info("After the message {} is received from {}", message, channel);
        return message;
    }
}
