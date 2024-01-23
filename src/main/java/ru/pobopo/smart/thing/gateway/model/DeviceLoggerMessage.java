package ru.pobopo.smart.thing.gateway.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;

@Data
@ToString
public class DeviceLoggerMessage {
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;
    private DeviceInfo device;
    private Level level;
    private String tag;
    private String message;
    private DeviceLogSource source;

    public static DeviceLoggerMessage parse(String message) {
        DeviceLoggerMessage deviceLoggerMessage = new DeviceLoggerMessage();
        if (StringUtils.isBlank(message)) {
            return deviceLoggerMessage;
        }

        String[] splited = message.split("_&_*");
        if (splited.length != 5) {
            return deviceLoggerMessage;
        }

        deviceLoggerMessage.setDateTime(LocalDateTime.now());
        deviceLoggerMessage.setDevice(
            new DeviceInfo(splited[0], splited[1])
        );
        String level = splited[2];
        if (StringUtils.isBlank(level)) {
            level = "INFO";
        }
        deviceLoggerMessage.setLevel(Level.valueOf(level));
        deviceLoggerMessage.setTag(splited[3]);
        deviceLoggerMessage.setMessage(splited[4].trim());
        return deviceLoggerMessage;
    }
}
