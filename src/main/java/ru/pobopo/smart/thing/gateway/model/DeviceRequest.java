package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class DeviceRequest {
    private DeviceInfo device;
    private String command;
    private Map<String, Object> params = new HashMap<>();
}
