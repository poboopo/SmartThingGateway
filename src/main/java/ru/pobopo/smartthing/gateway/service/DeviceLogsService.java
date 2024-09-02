package ru.pobopo.smartthing.gateway.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.jobs.BackgroundJob;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.*;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Service
@RequiredArgsConstructor
public class DeviceLogsService implements BackgroundJob {
    public static final String DEVICES_LOGS_TOPIC = DEVICES_TOPIC + "/logs";
    private final Logger log = LoggerFactory.getLogger("device-logs");

    private final BlockingQueue<DeviceLoggerMessage> processQueue = new LinkedBlockingQueue<>();
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
            return;
        }
        processQueue.add(message);
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                DeviceLoggerMessage message = processQueue.take();
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
