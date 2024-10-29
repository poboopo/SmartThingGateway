package ru.pobopo.smartthing.gateway.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.event.CloudLoginEvent;
import ru.pobopo.smartthing.gateway.event.CloudLogoutEvent;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudNotificationConsumer implements DeviceNotificationConsumer {
    private final CloudApiService cloudService;

    private final Queue<DeviceNotification> queue = new LinkedBlockingQueue<>(10);

    @Override
    public void accept(DeviceNotification notification) {
        if (cloudService.getCloudConfig() == null) {
            return;
        }

        try {
            cloudService.notification(notification);
        } catch (Exception e) {
            log.error("Failed to send notification in cloud, adding notification in queue (error message = {})", e.getMessage());
            if (!queue.offer(notification)) {
                log.error("Notification queue capacity reached");
            }
        }
    }

    @EventListener
    public void loginEvent(CloudLoginEvent event) {
        processQueue();
    }

    @EventListener
    public void logoutEvent(CloudLogoutEvent event) {
        if (queue.isEmpty()) {
            return;
        }
        queue.clear();
        log.warn("Cloud notifications queue cleared");
    }

    @Scheduled(fixedDelay = 10000)
    public void processQueue() {
        if (queue.isEmpty()) {
            return;
        }

        try {
            log.info("Trying to send notifications from queue");
            while (!queue.isEmpty()) {
                DeviceNotification notification = queue.peek();
                cloudService.notification(notification);
                queue.remove();
            }
        } catch (Exception e) {
            log.error("Failed to process notification queue (error message={})", e.getMessage());
        }
    }
}
