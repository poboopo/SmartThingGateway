package ru.pobopo.smartthing.gateway.service.device.api;

import ru.pobopo.smartthing.model.device.DeviceInfo;

public interface DeviceApi {
    boolean accept(DeviceInfo deviceInfo);
}
