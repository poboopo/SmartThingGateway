package ru.pobopo.smartthing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Device settings")
public class DeviceSettings {
    @Schema(description = "Settings name. Equals to file name.")
    private String name;
    @Schema(description = "Settings in json format")
    private String value;
}
