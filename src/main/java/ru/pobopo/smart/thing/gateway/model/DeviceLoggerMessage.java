package ru.pobopo.smart.thing.gateway.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
@ToString
public class DeviceLoggerMessage {
    private LocalDateTime receiveDate;
    private DeviceInfo deviceInfo;
    private String logLevel;
    private String tag;
    private String message;

    public static DeviceLoggerMessage fromMulticastMessage(String message) {
        DeviceLoggerMessage deviceLoggerMessage = new DeviceLoggerMessage();
        if (StringUtils.isBlank(message)) {
            return deviceLoggerMessage;
        }

        String[] splited = message.split("[$]");
        if (splited.length != 5) {
            return deviceLoggerMessage;
        }

        deviceLoggerMessage.setReceiveDate(LocalDateTime.now());
        deviceLoggerMessage.setDeviceInfo(
            new DeviceInfo(splited[0], splited[1])
        );
        deviceLoggerMessage.setLogLevel(splited[2]);
        deviceLoggerMessage.setTag(splited[3]);
        deviceLoggerMessage.setMessage(splited[4].trim());
        return deviceLoggerMessage;
    }
}
