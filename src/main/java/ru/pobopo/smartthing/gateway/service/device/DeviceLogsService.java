package ru.pobopo.smartthing.gateway.service.device;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.model.logs.DeviceLogsFilter;
import ru.pobopo.smartthing.gateway.service.job.BackgroundJob;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Service
@RequiredArgsConstructor
public class DeviceLogsService implements BackgroundJob {
    public static final String DEVICES_LOGS_TOPIC = DEVICES_TOPIC + "/logs";
    private final Logger log = LoggerFactory.getLogger("device-logs");

    private final BlockingQueue<DeviceLoggerMessage> processQueue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedDeque<DeviceLoggerMessage> logsQueue = new ConcurrentLinkedDeque<>();

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${device.logs.cache.size:200}")
    private int cacheSize;
    @Value("${device.logs.level:INFO}")
    private Level logLevel;

    public List<DeviceLoggerMessage> getLogs(DeviceLogsFilter filter) {
        Stream<DeviceLoggerMessage> messageStream = logsQueue.stream();
        if (filter == null || filter.isEmpty()) {
            return messageStream.toList();
        }
        return messageStream.filter(message -> {
            if (filter.getLevel() != null && !message.getLevel().equals(filter.getLevel())) {
                return false;
            }
            if (StringUtils.isNotBlank(filter.getMessage()) && !StringUtils.contains(message.getMessage().toLowerCase(), filter.getMessage())) {
                return false;
            }
            if (StringUtils.isNotBlank(filter.getTag()) && !StringUtils.contains(message.getTag().toLowerCase(), filter.getTag())) {
                return false;
            }
            if (StringUtils.isBlank(filter.getDevice())) {
                return true;
            }
            String dev = message.getDevice().getName() + message.getDevice().getIp();
            return StringUtils.contains(dev.toLowerCase(), filter.getDevice());
        }).toList();
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
                logsQueue.addFirst(message);

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
