package ru.pobopo.smart.thing.gateway.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.pobopo.smart.thing.gateway.model.DeviceSettings;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "Update device settings")
public class UpdateDeviceSettings extends DeviceSettings {
    @Schema(description = "Old settings name")
    private String oldName;
}
