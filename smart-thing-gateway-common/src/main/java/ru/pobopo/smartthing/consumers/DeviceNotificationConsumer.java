package ru.pobopo.smartthing.consumers;

import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import java.util.function.Consumer;

public interface DeviceNotificationConsumer extends Consumer<DeviceNotification> {
}
