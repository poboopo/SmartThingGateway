package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static ru.pobopo.smart.thing.gateway.service.LogJobsService.DEVICES_LOGS_TOPIC;

@Service
public class DeviceLogsService {
    private final Logger log = LoggerFactory.getLogger("device-logs");
    private final ConcurrentLinkedQueue<DeviceLoggerMessage> logsQueue = new ConcurrentLinkedQueue<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final int cacheSize;
    private final Level logLevel;

    @Autowired
    public DeviceLogsService(SimpMessagingTemplate messagingTemplate, Environment env) {
        this.messagingTemplate = messagingTemplate;
        cacheSize = env.getProperty("device.logs.cache.max", Integer.class, 100);
        logLevel = env.getProperty("device.logs.level", Level.class, Level.INFO);

        log.info("Using logs cache size: {}, logs level: {}", cacheSize, logLevel);
    }

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
