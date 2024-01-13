package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class DeviceRequest {
    private DeviceInfo target;
    private String method;
    private Map<String, Object> params = new HashMap<>();
}
