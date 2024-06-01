package ru.pobopo.smartthing.gateway.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class DeviceLogsService {
    public static final String DEVICES_LOGS_TOPIC = "/devices/logs";
    private final Logger log = LoggerFactory.getLogger("device-logs");
    private final ConcurrentLinkedQueue<DeviceLoggerMessage> logsQueue = new ConcurrentLinkedQueue<>();

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${device.logs.cache.size:100}")
    private int cacheSize;
    @Value("${device.logs.level:INFO}")
    private Level logLevel;

    public List<DeviceLoggerMessage> getLogs() {
        return logsQueue.stream().toList();
    }

    public void addLog(DeviceLoggerMessage message) {
        if (message == null) {
            log.error("Message is null!");
            return;
        }

        if (logLevel.compareTo(message.getLevel()) < 0) {
            return;
        }

        log.atLevel(message.getLevel()).log(message.toString());
        if (logsQueue.size() > cacheSize) {
            logsQueue.remove();
        }
        logsQueue.add(message);

        messagingTemplate.convertAndSend(
                DEVICES_LOGS_TOPIC,
                message
        );
    }
}
