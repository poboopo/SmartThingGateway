package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.gateway.GatewayNotificationConsumer;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.NOTIFICATION_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsNotificationConsumer implements GatewayNotificationConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void consume(GatewayNotification notification) {
        messagingTemplate.convertAndSend(
                NOTIFICATION_TOPIC,
                notification
        );
    }
}
