package ru.pobopo.smart.thing.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    public static final String NOTIFICATION_TOPIC = "/notification";

    private final SimpMessagingTemplate messagingTemplate;
    private final CloudService cloudService;


    public void sendNotification(GatewayNotification request) {
        log.info("Device {} sending notification {}", request.getDevice(), request.getNotification());

        messagingTemplate.convertAndSend(
                NOTIFICATION_TOPIC,
                request
        );
        try {
            cloudService.notification(request);
            log.info("Notification sent to cloud!");
        } catch (Exception exception) {
            log.error("Failed to send notification in cloud: {}", exception.getMessage());
        }
    }
}
