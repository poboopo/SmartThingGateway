package ru.pobopo.smartthing.gateway.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;
import ru.pobopo.smartthing.model.DeviceInfo;

@Data
@ToString
@Schema(description = "Device log message")
public class DeviceLoggerMessage {
    @Schema(description = "Message receive date time")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;
    @Schema(description = "Sender device info")
    private DeviceInfo device;
    @Schema(description = "Log message level")
    private Level level = Level.INFO;
    @Schema(description = "Log message tag")
    private String tag;
    @Schema(description = "Log message")
    private String message;
    @Schema(description = "Where messages were sent - tcp or multicast channel")
    private DeviceLogSource source;

    public static DeviceLoggerMessage parse(String address, String message) {
        DeviceLoggerMessage deviceLoggerMessage = new DeviceLoggerMessage();
        if (StringUtils.isBlank(message)) {
            return deviceLoggerMessage;
        }

        String[] splited = message.split("_&_*");
        if (splited.length != 4) {
            return deviceLoggerMessage;
        }

        deviceLoggerMessage.setDateTime(LocalDateTime.now());
        deviceLoggerMessage.setDevice(
            new DeviceInfo(address, splited[0])
        );
        int level = Integer.parseInt(splited[1]);
        deviceLoggerMessage.setLevel(Level.intToLevel(level));
        deviceLoggerMessage.setTag(splited[2]);
        deviceLoggerMessage.setMessage(splited[3].trim());
        return deviceLoggerMessage;
    }
}
