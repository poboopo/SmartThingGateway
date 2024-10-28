package ru.pobopo.smartthing.gateway.service.notification;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.gateway.repository.SavedDeviceNotification;
import ru.pobopo.smartthing.gateway.service.AsyncQueuedConsumersProcessor;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.DeviceNotification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final AsyncQueuedConsumersProcessor<DeviceNotificationConsumer, DeviceNotification> processor;
    private final FileRepository<SavedDeviceNotification> repository;

    public Collection<SavedDeviceNotification> getNotifications() {
        return repository.getAll();
    }

    public UUID sendNotification(DeviceNotification notification) {
        DeviceInfo info = notification.getDevice();
        if (info == null || StringUtils.isEmpty(info.getIp()) || StringUtils.isEmpty(info.getName())) {
            throw new ValidationException("Device info is required!");
        }
        if (notification.getNotification() == null || StringUtils.isEmpty(notification.getNotification().getMessage())) {
            throw new ValidationException("Notification message is required!");
        }

        notification.setDateTime(LocalDateTime.now());

        log.info("Saving notification from device: {}", notification);

        SavedDeviceNotification deviceNotification = SavedDeviceNotification.fromNotification(notification);
        deviceNotification.setId(UUID.randomUUID());
        repository.add(deviceNotification);
        log.info("Notification saved, id={}", deviceNotification.getId());

        if (processor.process(notification)) {
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
