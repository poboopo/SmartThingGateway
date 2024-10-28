package ru.pobopo.smartthing.gateway.repository;

import lombok.*;
import ru.pobopo.smartthing.annotation.FileRepoId;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.DeviceNotification;
import ru.pobopo.smartthing.model.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class SavedDeviceNotification extends DeviceNotification {
    @FileRepoId
    private UUID id;

    public SavedDeviceNotification(DeviceInfo device, Notification notification) {
        super(device, notification, LocalDateTime.now());
    }

    public static SavedDeviceNotification fromNotification(DeviceNotification notification) {
        return new SavedDeviceNotification(notification.getDevice(), notification.getNotification());
    }
}
