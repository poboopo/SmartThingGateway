package ru.pobopo.smartthing.consumers;

import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.function.Consumer;

public interface DeviceLogsConsumer extends Consumer<DeviceLoggerMessage> {
}
