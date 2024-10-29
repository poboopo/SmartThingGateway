package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.NOTIFICATION_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsNotificationConsumer implements DeviceNotificationConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(DeviceNotification notification) {
        log.info("Sending notification in ws {}", notification.getNotification().getMessage());
        messagingTemplate.convertAndSend(
                NOTIFICATION_TOPIC,
                notification
        );
    }
}
