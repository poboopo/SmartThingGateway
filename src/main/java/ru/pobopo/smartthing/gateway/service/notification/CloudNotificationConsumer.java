package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.GatewayNotificationConsumer;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudNotificationConsumer implements GatewayNotificationConsumer {
    private final CloudApiService cloudService;

    @Override
    public void consume(GatewayNotification notification) {
        cloudService.notification(notification);
        log.info("Notification sent to cloud!");
    }
}
