package demo.websocket.server.example4.controller;

import demo.websocket.server.example4.domain.Performance;
import demo.websocket.server.example4.service.PerformanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
public class PerformanceController implements ApplicationListener<BrokerAvailabilityEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceController.class);

    private final PerformanceService performanceService;

    private final MessageSendingOperations<String> messageSendingOperations;

    private final AtomicBoolean brokerAvailable = new AtomicBoolean(false);

    public PerformanceController(PerformanceService performanceService, MessageSendingOperations<String> messageSendingOperations) {
        this.performanceService = performanceService;
        this.messageSendingOperations = messageSendingOperations;
    }

    @SubscribeMapping("/names")
    public List<String> getNames() {
        return Arrays.asList(
                "committedVirtualMemorySize",
                "totalPhysicalMemorySize",
                "freePhysicalMemorySize",
                "totalSwapSpaceSize",
                "freePhysicalMemorySize"
        );
    }

    @MessageMapping("/request")
    @SendTo("/queue/performance")
    public Performance onDemandPerformance() {
        return performanceService.getPerformance();
    }

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        logger.info("Broker availability event: {}", event);
        brokerAvailable.set(event.isBrokerAvailable());
        logger.info("Broker is available: {}", brokerAvailable.get());
    }

    @Scheduled(fixedDelay = 5000)
    public void periodicPerformance() {
        if (brokerAvailable.get()) {
            messageSendingOperations.convertAndSend("/topic/performance", performanceService.getPerformance());
        }
    }
}