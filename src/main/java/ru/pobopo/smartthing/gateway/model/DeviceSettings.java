package ru.pobopo.smartthing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Data
@Schema(description = "Device settings")
@Builder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSettings {
    @Schema(description = "Settings id")
    private UUID id;
    @Schema(description = "Settings name")
    private String name;
    @Schema(description = "Settings in json format")
    private String value;
}
