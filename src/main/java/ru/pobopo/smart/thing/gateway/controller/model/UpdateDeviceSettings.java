package ru.pobopo.smart.thing.gateway.controller.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.pobopo.smart.thing.gateway.model.DeviceSettings;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateDeviceSettings extends DeviceSettings {
    private String oldName;
}
