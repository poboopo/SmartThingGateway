package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.gateway.GatewayNotificationConsumer;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

import java.util.List;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.NOTIFICATION_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final List<GatewayNotificationConsumer> consumerList;

    public void sendNotification(GatewayNotification notification) {
        log.info("Device {} sending notification {}", notification.getDevice(), notification.getNotification());

        for (GatewayNotificationConsumer consumer: consumerList) {
            try {
                consumer.consume(notification);
            } catch (Exception e) {
                log.error("Notification consumer error", e);
            }
        }
    }
}
