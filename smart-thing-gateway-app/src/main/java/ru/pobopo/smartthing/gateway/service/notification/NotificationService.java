package ru.pobopo.smartthing.gateway.service.notification;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.gateway.service.AsyncQueuedConsumersProcessor;
import ru.pobopo.smartthing.model.device.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final AsyncQueuedConsumersProcessor<DeviceNotificationConsumer, DeviceNotification> processor;
    private final FileRepository<DeviceNotification> repository;

    public Collection<DeviceNotification> getNotifications() {
        return repository.getAll();
    }

    public UUID sendNotification(DeviceNotification deviceNotification) {
        DeviceInfo info = deviceNotification.getDevice();
        if (info == null || StringUtils.isEmpty(info.getIp()) || StringUtils.isEmpty(info.getName())) {
            throw new ValidationException("Device info is required!");
        }
        if (deviceNotification.getNotification() == null || StringUtils.isEmpty(deviceNotification.getNotification().getMessage())) {
            throw new ValidationException("Notification message is required!");
        }

        deviceNotification.setDateTime(LocalDateTime.now());
        deviceNotification.setId(UUID.randomUUID());

        log.info("Saving notification from device: {}", deviceNotification);

        repository.add(deviceNotification);
        log.info("Notification saved, id={}", deviceNotification.getId());

        if (processor.process(deviceNotification)) {
            log.info("Notification added in processing queue");
        } else {
            log.error("Failed to add notification in processing queue");
        }

        return deviceNotification.getId();
    }

    public boolean markSeen(UUID id) {
        if (repository.findById(id).isPresent()) {
            repository.delete(id);
            log.info("Notification id={} deleted", id);
            return true;
        }
        return false;
    }
}
