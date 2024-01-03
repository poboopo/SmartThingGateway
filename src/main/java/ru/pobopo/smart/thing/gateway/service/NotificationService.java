package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;

@Component
@Slf4j
public class NotificationService {
    private static final String NOTIFICATION_TOPIC = "/notification";

    private final MessageBrokerService messageBrokerService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(MessageBrokerService messageBrokerService, SimpMessagingTemplate messagingTemplate) {
        this.messageBrokerService = messageBrokerService;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotification(SendNotificationRequest request) {
        log.info("Device {} sending notification {}", request.getDevice(), request.getNotification());

        messagingTemplate.convertAndSend(
                NOTIFICATION_TOPIC,
                request
        );
        if (messageBrokerService.sendNotification(request)) {
            log.info("Notification sent to cloud!");
        }
    }
}
