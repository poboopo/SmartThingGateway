package ru.pobopo.smartthing.gateway.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Service
@RequiredArgsConstructor
public class DeviceLogsService {
    public static final String DEVICES_LOGS_TOPIC = DEVICES_TOPIC + "/logs";
    private final Logger log = LoggerFactory.getLogger("device-logs");
    private final ConcurrentLinkedQueue<DeviceLoggerMessage> logsQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${device.logs.cache.size:100}")
    private int cacheSize;
    @Value("${device.logs.level:INFO}")
    private Level logLevel;

    public List<DeviceLoggerMessage> getLogs() {
        return logsQueue.stream().toList();
    }

    public void addLog(DeviceLoggerMessage message) {
        executorService.submit(() -> processLog(message));
    }

    private void processLog(DeviceLoggerMessage message) {
        log.atLevel(message.getLevel()).log(message.toString());
        if (logsQueue.size() > cacheSize) {
            logsQueue.remove();
        }
        logsQueue.add(message);

        try {
            messagingTemplate.convertAndSend(
                    DEVICES_LOGS_TOPIC,
                    message
            );
        } catch (Exception exception) {
            log.error("Failed to send log message in topics", exception);
        }
    }
}
