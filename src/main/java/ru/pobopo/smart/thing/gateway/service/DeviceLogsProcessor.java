package ru.pobopo.smart.thing.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static ru.pobopo.smart.thing.gateway.service.LogJobsService.DEVICES_LOGS_TOPIC;

@Service
@RequiredArgsConstructor
public class DeviceLogsProcessor {
    private final int MAX_LOGS_SIZE = 100;

    private final Logger deviceLogs = LoggerFactory.getLogger("device-logs");
    private final ConcurrentLinkedQueue<DeviceLoggerMessage> logsQueue = new ConcurrentLinkedQueue<>();

    private final SimpMessagingTemplate messagingTemplate;

    public List<DeviceLoggerMessage> getLogs() {
        return logsQueue.stream().toList();
    }

    public void addLog(DeviceLoggerMessage message) {
        if (message == null) {
            deviceLogs.error("Message is null!");
            return;
        }

        deviceLogs.atLevel(message.getLevel()).log(message.toString());
        if (logsQueue.size() > MAX_LOGS_SIZE) {
            logsQueue.remove();
        }
        logsQueue.add(message);

        messagingTemplate.convertAndSend(
                DEVICES_LOGS_TOPIC,
                message
        );
    }
}
