package demo.websocket.server.example3;

import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ScheduledController {

    private final MessageSendingOperations<String> messageSendingOperations;

    public ScheduledController(MessageSendingOperations<String> messageSendingOperations) {
        this.messageSendingOperations = messageSendingOperations;
    }

    @Scheduled(fixedDelay = 10000)
    public void sendPeriodicMessages() {
        String broadcast = String.format("server periodic message %s via the broker", LocalTime.now());
        this.messageSendingOperations.convertAndSend("/topic/periodic", broadcast);
    }
}
