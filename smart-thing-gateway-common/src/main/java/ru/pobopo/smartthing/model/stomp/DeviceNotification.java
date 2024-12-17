package ru.pobopo.smartthing.model.stomp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.annotation.FileRepoId;
import ru.pobopo.smartthing.model.Notification;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceNotification {
    @FileRepoId
    private UUID id;
    private DeviceInfo device;
    private Notification notification;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime = LocalDateTime.now();
}
