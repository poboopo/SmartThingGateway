package ru.pobopo.smart.thing.gateway.device.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

@Slf4j
public abstract class DeviceApi {
    public abstract boolean accept(DeviceRequest request);

    public abstract DeviceResponse getInfo(DeviceInfo info);

    public DeviceResponse getActions(DeviceInfo info) {
        log.info("Calling default getActions method");
        return new DeviceResponse(HttpStatus.OK, "[]", null);
    }
    public DeviceResponse callAction(DeviceInfo info, String action) {
        log.info("Calling default callAction method");
        return new DeviceResponse(HttpStatus.BAD_REQUEST, "There is no action handler", null);
    }

    public DeviceResponse getSensors(DeviceInfo info) {
        log.info("Calling default getSensors method");
        return new DeviceResponse(HttpStatus.OK, "[]", null);
    }

    public DeviceResponse getStates(DeviceInfo info) {
        log.info("Calling default getStates method");
        return new DeviceResponse(HttpStatus.OK, "[]", null);
    }
}
