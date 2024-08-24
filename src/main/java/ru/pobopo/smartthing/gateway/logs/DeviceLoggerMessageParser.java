package ru.pobopo.smartthing.gateway.logs;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.DeviceLogSource;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeviceLoggerMessageParser {
    private final static String GROUP_IP = "ip";
    private final static String GROUP_NAME = "name";
    private final static String GROUP_LEVEL = "level";
    private final static String GROUP_TAG = "tag";
    private final static String GROUP_MESSAGE = "message";

    private final static Pattern tcpPattern = Pattern.compile(
            "(?<" + GROUP_NAME + ">.+)[&](?<" + GROUP_LEVEL + ">\\d+)[&](?<" + GROUP_TAG + ">.+)[&](?<" + GROUP_MESSAGE + ">.+)\\Z"
    );
    private final static Pattern multicastPattern = Pattern.compile(
            "(?<" + GROUP_IP + ">.+)[&](?<" + GROUP_NAME + ">.+)[&](?<" + GROUP_LEVEL + ">\\d+)[&](?<" + GROUP_TAG + ">.+)[&](?<" + GROUP_MESSAGE + ">.+)\\Z"
    );

    @Nullable
    public DeviceLoggerMessage parse(DeviceLogSource source, String message, String ip) {
        if (StringUtils.isBlank(message) || source == null) {
            return null;
        }

        Matcher matcher = null;
        switch (source) {
            case TCP -> matcher = tcpPattern.matcher(message);
            case MULTICAST -> matcher = multicastPattern.matcher(message);
        }
        if (matcher == null || !matcher.find()) {
            return null;
        }

        return DeviceLoggerMessage.builder()
                .device(new DeviceInfo(
                        DeviceLogSource.MULTICAST.equals(source) ? matcher.group(GROUP_IP) : ip,
                        matcher.group(GROUP_NAME))
                )
                .level(Level.intToLevel(Integer.parseInt(matcher.group(GROUP_LEVEL))))
                .tag(matcher.group(GROUP_TAG))
                .message(matcher.group(GROUP_MESSAGE))
                .source(source)
                .dateTime(LocalDateTime.now())
                .build();
    }
}
