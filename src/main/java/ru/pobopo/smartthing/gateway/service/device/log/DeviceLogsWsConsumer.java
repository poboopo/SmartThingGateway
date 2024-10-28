package ru.pobopo.smartthing.gateway.service.device.log;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.consumers.DeviceLogsConsumer;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import static ru.pobopo.smartthing.gateway.service.device.log.DeviceLogsCacheService.DEVICES_LOGS_TOPIC;

@Component
@RequiredArgsConstructor
public class DeviceLogsWsConsumer implements DeviceLogsConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(DeviceLoggerMessage message) {
        messagingTemplate.convertAndSend(
                DEVICES_LOGS_TOPIC,
                message
        );
    }
}
