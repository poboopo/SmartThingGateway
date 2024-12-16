package ru.pobopo.smartthing.gateway.model.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.annotation.FileRepoId;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(description = "Device settings")
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSettingsDump {
    @FileRepoId
    @Schema(description = "Settings id")
    private UUID id;
    @Schema(description = "Device")
    private DeviceInfo device;
    @Schema(description = "Device settings dump")
    private String dump;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime creationDateTime;
}
