package ru.pobopo.smartthing.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Data
@ToString
@Schema(description = "Device log message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceLoggerMessage {
    private final static Pattern messagePattern = Pattern.compile("(?<name>.+)[&](?<level>.+)[&](?<tag>.+)[&](?<message>.+)\\Z");
    private static final Logger log = LoggerFactory.getLogger(DeviceLoggerMessage.class);

    @Schema(description = "Message receive date time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
}
