package ru.pobopo.smartthing.consumers;

import ru.pobopo.smartthing.model.stomp.GatewayNotification;

import java.util.function.Consumer;

public interface GatewayNotificationConsumer extends Consumer<GatewayNotification> {
}
