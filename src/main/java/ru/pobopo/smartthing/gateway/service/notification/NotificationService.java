package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.GatewayNotificationConsumer;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

import java.util.List;

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
