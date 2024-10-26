package ru.pobopo.smartthing.gateway.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.model.DeviceNotification;

import java.util.List;

@Component
@Slf4j
public class NotificationService {
    private final List<DeviceNotificationConsumer> consumerList;

    public NotificationService(
            @Qualifier("notification-consumers") List<DeviceNotificationConsumer> consumerList
    ) {
        this.consumerList = consumerList;
    }

    public void sendNotification(DeviceNotification notification) {
        log.info("Device {} sending notification {}", notification.getDevice(), notification.getNotification());

        for (DeviceNotificationConsumer consumer: consumerList) {
            try {
                consumer.consume(notification);
            } catch (Exception e) {
                log.error("Notification consumer error: {}", e.getMessage());
            }
        }
    }
}
