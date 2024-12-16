package ru.pobopo.smartthing.gateway.service.device.log;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.consumers.DeviceLogsConsumer;
import ru.pobopo.smartthing.gateway.model.logs.DeviceLogsFilter;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Service
@RequiredArgsConstructor
public class DeviceLogsCacheService implements DeviceLogsConsumer {
    public static final String DEVICES_LOGS_TOPIC = DEVICES_TOPIC + "/logs";
    private final Logger log = LoggerFactory.getLogger("device-logs");

    private final ConcurrentLinkedDeque<DeviceLoggerMessage> logsCache = new ConcurrentLinkedDeque<>();

    @Value("${device.logs.cache.size:200}")
    private int cacheSize;
    @Value("${device.logs.level:INFO}")
    private Level logLevel;

    public List<DeviceLoggerMessage> getLogs(DeviceLogsFilter filter) {
        Stream<DeviceLoggerMessage> messageStream = logsCache.stream();
        if (filter == null || filter.isEmpty()) {
            return messageStream.toList();
        }
        return messageStream.filter(message -> {
            if (filter.getLevel() != null && message.getLevel().toInt() < filter.getLevel().toInt()) {
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

    @Override
    public void accept(DeviceLoggerMessage message) {
        // todo move from here?
        log.atLevel(message.getLevel()).log(message.toString());
        if (logsCache.size() > cacheSize) {
            logsCache.remove();
        }
        logsCache.addFirst(message);
    }
}
