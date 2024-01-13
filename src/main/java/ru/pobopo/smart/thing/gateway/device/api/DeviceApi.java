package ru.pobopo.smart.thing.gateway.device.api;

import ru.pobopo.smart.thing.gateway.model.DeviceRequest;

public interface DeviceApi {
    boolean accept(DeviceRequest request);
}
