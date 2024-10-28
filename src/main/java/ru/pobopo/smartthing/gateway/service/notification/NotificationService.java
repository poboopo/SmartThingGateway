package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.service.AsyncQueuedConsumersProcessor;
import ru.pobopo.smartthing.model.DeviceNotification;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final AsyncQueuedConsumersProcessor<DeviceNotificationConsumer, DeviceNotification> processor;

    public void sendNotification(DeviceNotification notification) {
        processor.process(notification);
    }
}
