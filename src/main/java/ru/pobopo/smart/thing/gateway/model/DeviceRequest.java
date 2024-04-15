package ru.pobopo.smart.thing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
@Schema(description = "Device api request")
public class DeviceRequest {
    @Schema(description = "Target device info")
    private DeviceInfo device;
    @Schema(description = "Api method name")
    private String command;
    @Schema(description = "Api method params")
    private Map<String, Object> params = new HashMap<>();
}
