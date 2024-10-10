package ru.pobopo.smartthing.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.NOTIFICATION_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudApiService cloudService;


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
